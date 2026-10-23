package com.axxelium.servicedesk;

import static com.axxelium.servicedesk.ApiModels.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/engineers")
@Tag(name = "Engineers", description = "Инженеры техподдержки")
public class EngineerController {
    private final DeskService service;

    public EngineerController(DeskService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<EngineerResponse> create(@Valid @RequestBody CreateEngineer request) {
        EngineerResponse response = service.createEngineer(request);
        return ResponseEntity.created(URI.create("/api/v1/engineers/" + response.id())).body(response);
    }

    @GetMapping
    public PageResponse<EngineerResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.listEngineers(page, size);
    }

    @GetMapping("/{id}")
    public EngineerResponse get(@PathVariable @Positive long id) {
        return service.getEngineer(id);
    }
}
