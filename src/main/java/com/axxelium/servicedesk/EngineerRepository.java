package com.axxelium.servicedesk;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineerRepository extends JpaRepository<Engineer, Long> {
    boolean existsByEmail(String email);
}
