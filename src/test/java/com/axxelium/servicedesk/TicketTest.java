package com.axxelium.servicedesk;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TicketTest {
    private Ticket ticket() {
        return new Ticket("VPN is down", "Cannot connect to the office", "user@example.com");
    }

    @Test
    void followsLifecycleAndCanBeReopened() {
        Ticket ticket = ticket();
        Engineer engineer = new Engineer("Dana", "dana@example.com");
        ticket.assign(engineer);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        ticket.changeStatus(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
        ticket.changeStatus(TicketStatus.OPEN);
        assertThat(ticket.getResolvedAt()).isNull();
        assertThat(ticket.getAssignee()).isSameAs(engineer);
    }

    @Test
    void cannotStartWithoutEngineer() {
        assertThatThrownBy(() -> ticket().changeStatus(TicketStatus.IN_PROGRESS))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotSkipWorkOrGoBackFromInProgress() {
        Ticket ticket = ticket();
        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.RESOLVED))
                .isInstanceOf(ConflictException.class);
        ticket.assign(new Engineer("Dana", "dana@example.com"));
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        assertThatThrownBy(() -> ticket.changeStatus(TicketStatus.OPEN))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotUnassignInProgressOrReassignResolved() {
        Ticket ticket = ticket();
        Engineer engineer = new Engineer("Dana", "dana@example.com");
        ticket.assign(engineer);
        ticket.changeStatus(TicketStatus.IN_PROGRESS);
        assertThatThrownBy(() -> ticket.assign(null)).isInstanceOf(ConflictException.class);
        ticket.changeStatus(TicketStatus.RESOLVED);
        assertThatThrownBy(() -> ticket.assign(engineer)).isInstanceOf(ConflictException.class);
    }

    @Test
    void sameStatusIsIdempotent() {
        Ticket ticket = ticket();
        var updatedAt = ticket.getUpdatedAt();
        ticket.changeStatus(TicketStatus.OPEN);
        assertThat(ticket.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
