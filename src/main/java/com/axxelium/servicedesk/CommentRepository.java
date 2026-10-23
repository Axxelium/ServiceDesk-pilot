package com.axxelium.servicedesk;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<TicketComment, Long> {
    Page<TicketComment> findByTicketId(long ticketId, Pageable pageable);
}
