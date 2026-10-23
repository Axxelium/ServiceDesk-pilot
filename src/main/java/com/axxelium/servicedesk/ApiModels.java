package com.axxelium.servicedesk;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/** HTTP models are separate from JPA entities to keep the API contract explicit. */
public final class ApiModels {
    private ApiModels() {}

    public record CreateEngineer(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email) {}

    public record EngineerResponse(Long id, String name, String email, Instant createdAt) {
        static EngineerResponse from(Engineer engineer) {
            return new EngineerResponse(engineer.getId(), engineer.getName(),
                    engineer.getEmail(), engineer.getCreatedAt());
        }
    }

    public record CreateTicket(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 10000) String description,
            @NotBlank @Email @Size(max = 254) String requesterEmail) {}

    public record ChangeStatus(@NotNull TicketStatus status) {}
    public record AssignEngineer(@NotNull @Positive Long engineerId) {}
    public record AddComment(
            @NotBlank @Size(max = 100) String author,
            @NotBlank @Size(max = 5000) String body) {}

    public record TicketResponse(Long id, String title, String description, String requesterEmail,
            TicketStatus status, EngineerResponse assignee, Instant createdAt,
            Instant updatedAt, Instant resolvedAt, long version) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                    ticket.getRequesterEmail(), ticket.getStatus(),
                    ticket.getAssignee() == null ? null : EngineerResponse.from(ticket.getAssignee()),
                    ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt(), ticket.getVersion());
        }
    }

    public record CommentResponse(Long id, String author, String body, Instant createdAt) {
        static CommentResponse from(TicketComment comment) {
            return new CommentResponse(comment.getId(), comment.getAuthor(),
                    comment.getBody(), comment.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}
