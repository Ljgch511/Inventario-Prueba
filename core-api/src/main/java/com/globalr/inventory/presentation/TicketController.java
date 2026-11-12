package com.globalr.inventory.presentation;

import com.globalr.inventory.application.AssetService;
import com.globalr.inventory.application.TicketService;
import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.enums.DomainEnums.TicketStatus;
import com.globalr.inventory.domain.enums.DomainEnums.TicketType;
import com.globalr.inventory.domain.model.GeneratedDocumentEntity;
import com.globalr.inventory.domain.model.MaintenanceEntity;
import com.globalr.inventory.domain.model.TicketEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TicketController {

    private final TicketService ticketService;
    private final AssetService assetService;

    public TicketController(TicketService ticketService, AssetService assetService) {
        this.ticketService = ticketService;
        this.assetService = assetService;
    }

    @GetMapping("/tickets")
    public List<Map<String, Object>> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketType type
    ) {
        return ticketService.listTickets(status, type);
    }

    @PostMapping("/tickets")
    public TicketEntity createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @PostMapping("/tickets/{ticketId}/approve")
    public TicketEntity approveTicket(
            @PathVariable UUID ticketId,
            @RequestBody(required = false) ApproveTicketRequest request
    ) {
        return ticketService.approveTicket(ticketId, request);
    }

    @PostMapping("/tickets/{ticketId}/reject")
    public TicketEntity rejectTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody RejectTicketRequest request
    ) {
        return ticketService.rejectTicket(ticketId, request);
    }

    @PostMapping("/tickets/{ticketId}/complete-transport")
    public TicketEntity completeTransport(
            @PathVariable UUID ticketId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        return ticketService.completeTransport(ticketId, body != null ? body.get("comment") : null);
    }

    @PostMapping("/maintenances/{maintenanceId}/complete")
    public MaintenanceEntity completeMaintenance(
            @PathVariable UUID maintenanceId,
            @Valid @RequestBody CompleteMaintenanceRequest request
    ) {
        return ticketService.completeMaintenance(maintenanceId, request);
    }

    @PostMapping("/documents/generate")
    public GeneratedDocumentEntity generateActa(@Valid @RequestBody GenerateActaRequest request) {
        return ticketService.generateActa(request);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadActa(@PathVariable UUID documentId) {
        byte[] bytes = assetService.downloadDocumentBytes(documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"acta-globalr.pdf\"")
                .body(bytes);
    }
}
