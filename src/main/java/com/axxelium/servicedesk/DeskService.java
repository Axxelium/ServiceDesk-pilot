package com.axxelium.servicedesk;

import static com.axxelium.servicedesk.ApiModels.*;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeskService {
    private final EngineerRepository engineers;
    private final TicketRepository tickets;
    private final CommentRepository comments;

    public DeskService(EngineerRepository engineers, TicketRepository tickets, CommentRepository comments) {
        this.engineers = engineers;
        this.tickets = tickets;
        this.comments = comments;
    }

    @Transactional
    public EngineerResponse createEngineer(CreateEngineer request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (engineers.existsByEmail(email)) {
            throw new ConflictException("An engineer with this email already exists");
        }
        return EngineerResponse.from(engineers.saveAndFlush(new Engineer(request.name(), email)));
    }

    public PageResponse<EngineerResponse> listEngineers(int page, int size) {
        return PageResponse.from(engineers.findAll(PageRequest.of(page, size, Sort.by("id")))
                .map(EngineerResponse::from));
    }

    public EngineerResponse getEngineer(long id) {
        return EngineerResponse.from(requireEngineer(id));
    }

    @Transactional
    public TicketResponse createTicket(CreateTicket request) {
        return TicketResponse.from(tickets.saveAndFlush(
                new Ticket(request.title(), request.description(), request.requesterEmail())));
    }

    public TicketResponse getTicket(long id) {
        return TicketResponse.from(requireTicket(id));
    }

    public PageResponse<TicketResponse> listTickets(TicketStatus status, Long assigneeId, int page, int size) {
        Specification<Ticket> filter = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (assigneeId != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId));
        }
        return PageResponse.from(tickets.findAll(filter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(TicketResponse::from));
    }

    @Transactional
    public TicketResponse changeStatus(long id, ChangeStatus request) {
        Ticket ticket = requireTicket(id);
        ticket.changeStatus(request.status());
        tickets.flush(); // Return the version incremented by optimistic locking.
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse assign(long id, long engineerId) {
        Ticket ticket = requireTicket(id);
        ticket.assign(requireEngineer(engineerId));
        tickets.flush();
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse unassign(long id) {
        Ticket ticket = requireTicket(id);
        ticket.assign(null);
        tickets.flush();
        return TicketResponse.from(ticket);
    }

    @Transactional
    public CommentResponse addComment(long ticketId, AddComment request) {
        Ticket ticket = requireTicket(ticketId);
        return CommentResponse.from(comments.saveAndFlush(
                new TicketComment(ticket, request.author(), request.body())));
    }

    public PageResponse<CommentResponse> listComments(long ticketId, int page, int size) {
        requireTicket(ticketId);
        return PageResponse.from(comments.findByTicketId(ticketId,
                PageRequest.of(page, size, Sort.by("createdAt", "id"))).map(CommentResponse::from));
    }

    private Ticket requireTicket(long id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
    }

    private Engineer requireEngineer(long id) {
        return engineers.findById(id).orElseThrow(() -> new NotFoundException("Engineer", id));
    }
}
