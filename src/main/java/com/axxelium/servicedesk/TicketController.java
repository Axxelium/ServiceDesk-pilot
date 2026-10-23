package com.axxelium.servicedesk;

import static com.axxelium.servicedesk.ApiModels.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Заявки и история комментариев")
public class TicketController {
    private final DeskService service;

    public TicketController(DeskService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicket request) {
        TicketResponse response = service.createTicket(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable @Positive long id) {
        return service.getTicket(id);
    }

    @GetMapping
    @Operation(summary = "Список заявок: фильтры по статусу и инженеру, новые сверху")
    public PageResponse<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @Positive Long assigneeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listTickets(status, assigneeId, page, size);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "OPEN → IN_PROGRESS → RESOLVED → OPEN; повторный статус ничего не меняет")
    public TicketResponse changeStatus(@PathVariable @Positive long id,
                                      @Valid @RequestBody ChangeStatus request) {
        return service.changeStatus(id, request);
    }

    @PutMapping("/{id}/assignee")
    public TicketResponse assign(@PathVariable @Positive long id,
                                 @Valid @RequestBody AssignEngineer request) {
        return service.assign(id, request.engineerId());
    }

    @DeleteMapping("/{id}/assignee")
    @Operation(summary = "Снять инженера с заявки в статусе OPEN")
    public TicketResponse unassign(@PathVariable @Positive long id) {
        return service.unassign(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable @Positive long id,
                                      @Valid @RequestBody AddComment request) {
        return service.addComment(id, request);
    }

    @GetMapping("/{id}/comments")
    public PageResponse<CommentResponse> listComments(@PathVariable @Positive long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listComments(id, page, size);
    }
}
