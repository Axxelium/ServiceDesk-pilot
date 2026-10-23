package com.axxelium.servicedesk;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 10000)
    private String description;
    @Column(nullable = false, length = 254)
    private String requesterEmail;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Engineer assignee;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    private Instant resolvedAt;
    @Version
    private long version;

    protected Ticket() {}

    public Ticket(String title, String description, String requesterEmail) {
        this.title = title.strip();
        this.description = description.strip();
        this.requesterEmail = requesterEmail.strip().toLowerCase(Locale.ROOT);
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void assign(Engineer engineer) {
        if (status == TicketStatus.RESOLVED) {
            throw new ConflictException("Reopen the ticket before changing its assignee");
        }
        if (engineer == null && status == TicketStatus.IN_PROGRESS) {
            throw new ConflictException("A ticket in progress must have an assignee");
        }
        assignee = engineer;
        updatedAt = Instant.now();
    }

    public void changeStatus(TicketStatus next) {
        if (status == next) return;
        boolean allowed = (status == TicketStatus.OPEN && next == TicketStatus.IN_PROGRESS)
                || (status == TicketStatus.IN_PROGRESS && next == TicketStatus.RESOLVED)
                || (status == TicketStatus.RESOLVED && next == TicketStatus.OPEN);
        if (!allowed) {
            throw new ConflictException("Transition " + status + " -> " + next + " is not allowed");
        }
        if (next == TicketStatus.IN_PROGRESS && assignee == null) {
            throw new ConflictException("Assign an engineer before starting work");
        }
        status = next;
        updatedAt = Instant.now();
        resolvedAt = next == TicketStatus.RESOLVED ? updatedAt : null;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequesterEmail() { return requesterEmail; }
    public TicketStatus getStatus() { return status; }
    public Engineer getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public long getVersion() { return version; }
}
