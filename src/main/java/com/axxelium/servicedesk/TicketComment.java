package com.axxelium.servicedesk;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @Column(nullable = false, length = 100)
    private String author;
    @Column(nullable = false, length = 5000)
    private String body;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketComment() {}

    public TicketComment(Ticket ticket, String author, String body) {
        this.ticket = ticket;
        this.author = author.strip();
        this.body = body.strip();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
