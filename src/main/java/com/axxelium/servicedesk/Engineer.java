package com.axxelium.servicedesk;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "engineers")
public class Engineer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Engineer() {}

    public Engineer(String name, String email) {
        this.name = name.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
}
