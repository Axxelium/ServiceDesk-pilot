package com.axxelium.servicedesk;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceDeskIntegrationTest {
    // Real PostgreSQL, including Flyway migrations. No Docker or H2 substitution.
    private static final EmbeddedPostgres POSTGRES = startPostgres();

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory entityManagers;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE ticket_comments, tickets, engineers RESTART IDENTITY CASCADE");
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        var result = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private long engineer() throws Exception {
        return postJson("/api/v1/engineers", Map.of("name", "Dana", "email", "dana@example.com"))
                .get("id").asLong();
    }

    private long ticket() throws Exception {
        return postJson("/api/v1/tickets", Map.of("title", "VPN is down",
                "description", "Cannot connect to office network", "requesterEmail", "user@example.com"))
                .get("id").asLong();
    }

    private void assign(long ticketId, long engineerId) throws Exception {
        mvc.perform(put("/api/v1/tickets/{id}/assignee", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("engineerId", engineerId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignee.id").value(engineerId));
    }

    @Test
    void fullWorkflowPersistsAssignmentStatusAndCommentHistory() throws Exception {
        long engineerId = engineer();
        long ticketId = ticket();
        assign(ticketId, engineerId);
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
        postJson("/api/v1/tickets/" + ticketId + "/comments", Map.of("author", "Dana", "body", "Checking VPN"));
        postJson("/api/v1/tickets/" + ticketId + "/comments", Map.of("author", "Dana", "body", "Certificate renewed"));
        mvc.perform(get("/api/v1/tickets/{id}/comments", ticketId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].body").value("Checking VPN"))
                .andExpect(jsonPath("$.content[1].body").value("Certificate renewed"));
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        mvc.perform(get("/api/v1/tickets/{id}", ticketId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedAt").isEmpty());
        mvc.perform(delete("/api/v1/tickets/{id}/assignee", ticketId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignee").isEmpty());
    }

    @Test
    void rejectsInvalidTransitionsAndMissingAssignee() throws Exception {
        long id = ticket();
        for (String next : new String[]{"IN_PROGRESS", "RESOLVED"}) {
            mvc.perform(patch("/api/v1/tickets/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("status", next))))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mvc.perform(get("/api/v1/tickets/{id}", id)).andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void validatesBodyEnumsAndPagination() throws Exception {
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"description\":\"test\",\"requesterEmail\":\"bad-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.requesterEmail").exists());
        mvc.perform(patch("/api/v1/tickets/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/tickets/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{}")).andExpect(status().isBadRequest());
        for (String query : new String[]{"size=101", "size=0", "page=-1", "status=UNKNOWN", "assigneeId=-1"}) {
            mvc.perform(get("/api/v1/tickets?" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/tickets/-1")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownResources() throws Exception {
        mvc.perform(get("/api/v1/tickets/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/engineers/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tickets/999/comments")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/tickets/999/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Dana\",\"body\":\"Hello\"}"))
                .andExpect(status().isNotFound());
        long id = ticket();
        mvc.perform(put("/api/v1/tickets/{id}/assignee", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"engineerId\":999}")).andExpect(status().isNotFound());
    }

    @Test
    void engineerEmailsAreUniqueIgnoringCase() throws Exception {
        engineer();
        mvc.perform(post("/api/v1/engineers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another Dana\",\"email\":\"DANA@EXAMPLE.COM\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM engineers", Integer.class)).isEqualTo(1);
    }

    @Test
    void filtersAndPaginatesTicketsDeterministically() throws Exception {
        long engineerId = engineer();
        long first = ticket();
        long second = ticket();
        ticket();
        assign(first, engineerId);
        assign(second, engineerId);
        mvc.perform(get("/api/v1/tickets").param("status", "OPEN")
                        .param("assigneeId", String.valueOf(engineerId)).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second));
        mvc.perform(get("/api/v1/tickets").param("assigneeId", String.valueOf(engineerId))
                        .param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content[0].id").value(first));
    }

    @Test
    void validatesCommentsAndKeepsThemAfterResolution() throws Exception {
        long id = ticket();
        mvc.perform(post("/api/v1/tickets/{id}/comments", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Dana\",\"body\":\" \"}")).andExpect(status().isBadRequest());
        assign(id, engineer());
        for (String next : new String[]{"IN_PROGRESS", "RESOLVED"}) {
            mvc.perform(patch("/api/v1/tickets/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("status", next)))).andExpect(status().isOk());
        }
        postJson("/api/v1/tickets/" + id + "/comments", Map.of("author", "User", "body", "Works now, thanks"));
        mvc.perform(get("/api/v1/tickets/{id}/comments", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/v1/tickets/{id}/assignee", id)).andExpect(status().isConflict());
    }

    @Test
    void optimisticLockRejectsLostUpdates() throws Exception {
        long id = ticket();
        long engineerId = engineer();
        var first = entityManagers.createEntityManager();
        var second = entityManagers.createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            Ticket a = first.find(Ticket.class, id);
            Ticket b = second.find(Ticket.class, id);
            a.assign(first.find(Engineer.class, engineerId));
            first.getTransaction().commit();
            b.assign(second.find(Engineer.class, engineerId));
            assertThatThrownBy(second::flush).isInstanceOf(OptimisticLockException.class);
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close();
            second.close();
        }
    }

    @Test
    void exposesOpenApiAndHealth() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ServiceDesk Pilot API"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class))
                .isEqualTo(1);
    }
}
