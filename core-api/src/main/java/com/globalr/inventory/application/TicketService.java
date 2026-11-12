package com.globalr.inventory.application;

import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.enums.DomainEnums.*;
import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.*;
import com.globalr.inventory.domain.repository.*;
import com.globalr.inventory.infrastructure.audit.AuditService;
import com.globalr.inventory.infrastructure.satellites.SatelliteClient;
import com.globalr.inventory.infrastructure.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketApprovalRepository approvalRepository;
    private final AssetRepository assetRepository;
    private final PersonnelRepository personnelRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final MovementTransportRepository movementRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final StoredPartRepository storedPartRepository;
    private final PartChangeRepository partChangeRepository;
    private final DecommissioningRepository decommissioningRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final SatelliteClient satelliteClient;
    private final AuditService auditService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketApprovalRepository approvalRepository,
            AssetRepository assetRepository,
            PersonnelRepository personnelRepository,
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            MovementTransportRepository movementRepository,
            MaintenanceRepository maintenanceRepository,
            StoredPartRepository storedPartRepository,
            PartChangeRepository partChangeRepository,
            DecommissioningRepository decommissioningRepository,
            GeneratedDocumentRepository documentRepository,
            SatelliteClient satelliteClient,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.approvalRepository = approvalRepository;
        this.assetRepository = assetRepository;
        this.personnelRepository = personnelRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.movementRepository = movementRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.storedPartRepository = storedPartRepository;
        this.partChangeRepository = partChangeRepository;
        this.decommissioningRepository = decommissioningRepository;
        this.documentRepository = documentRepository;
        this.satelliteClient = satelliteClient;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTickets(TicketStatus status, TicketType type) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        UUID requesterFilter = (principal.role() == Role.COMMON_USER) ? principal.userId() : null;
        Specification<TicketEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("ticketType"), type));
            }
            if (requesterFilter != null) {
                predicates.add(cb.equal(root.get("requesterUserId"), requesterFilter));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        List<TicketEntity> tickets = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        Map<UUID, AssetEntity> assetMap = new HashMap<>();
        for (AssetEntity a : assetRepository.findAll()) assetMap.put(a.getId(), a);
        Map<UUID, UserEntity> userMap = new HashMap<>();
        for (UserEntity u : userRepository.findByCountryId(principal.countryId())) userMap.put(u.getId(), u);
        Map<UUID, PersonnelEntity> persMap = new HashMap<>();
        for (PersonnelEntity p : personnelRepository.findAll()) persMap.put(p.getId(), p);

        return tickets.stream().map(t -> {
            AssetEntity asset = assetMap.get(t.getAssetId());
            UserEntity reqUser = userMap.get(t.getRequesterUserId());
            UserEntity approver = t.getApprovedByUserId() != null ? userMap.get(t.getApprovedByUserId()) : null;
            PersonnelEntity target = t.getTargetPersonnelId() != null ? persMap.get(t.getTargetPersonnelId()) : null;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("ticketCode", t.getTicketCode());
            row.put("ticketType", t.getTicketType());
            row.put("status", t.getStatus());
            row.put("priority", t.getPriority());
            row.put("title", t.getTitle());
            row.put("reason", t.getReason());
            row.put("assetId", t.getAssetId());
            row.put("assetNumber", asset != null ? asset.getAssetNumber() : "-");
            row.put("assetSummary", asset != null ? asset.getBrand() + " " + asset.getModel() : "-");
            row.put("requesterName", reqUser != null ? reqUser.getFullName() : t.getCreatedBy());
            row.put("targetPersonnelLabel", target != null ? target.displayLabel() : null);
            row.put("approverName", approver != null ? approver.getFullName() : null);
            row.put("transporterName", t.getTransporterName());
            row.put("originLocation", t.getOriginLocation());
            row.put("destinationLocation", t.getDestinationLocation());
            row.put("payloadJson", t.getPayloadJson());
            row.put("approvedAt", t.getApprovedAt());
            row.put("rejectionReason", t.getRejectionReason());
            row.put("createdAt", t.getCreatedAt());
            row.put("approvals", approvalRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()));
            return row;
        }).toList();
    }

    @Transactional
    public TicketEntity createTicket(CreateTicketRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = assetRepository.findByIdAndCountryId(req.assetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        TicketEntity ticket = new TicketEntity();
        ticket.setId(UUID.randomUUID());
        ticket.setCountryId(principal.countryId());
        ticket.setTicketCode("TCK-" + principal.countryCode() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        ticket.setTicketType(req.ticketType());
        ticket.setStatus(TicketStatus.PENDIENTE_APROBACION);
        ticket.setPriority(req.priority() != null ? req.priority() : TicketPriority.MEDIA);
        ticket.setTitle(req.title().trim());
        ticket.setReason(req.reason().trim());
        ticket.setAssetId(asset.getId());
        ticket.setRequesterUserId(principal.userId());
        ticket.setTargetPersonnelId(req.targetPersonnelId());
        ticket.setAssignedMaintenanceUserId(req.assignedMaintenanceUserId());
        ticket.setTransporterName(req.transporterName());
        ticket.setTransporterIdDoc(req.transporterIdDoc());
        ticket.setOriginLocation(req.originLocation() != null ? req.originLocation() : asset.getPhysicalLocation());
        ticket.setDestinationLocation(req.destinationLocation());
        ticket.setPayloadJson(req.payloadJson() != null ? req.payloadJson() : new HashMap<>());
        ticket.setCreatedBy(principal.username());
        ticket.setUpdatedBy(principal.username());
        ticket = ticketRepository.save(ticket);

        recordApprovalStep(ticket.getId(), principal.countryId(), principal.userId(), "CREADO", req.reason());
        auditService.record("TICKET_CREATE", "TICKET", ticket.getId().toString(), Map.of(
                "ticketCode", ticket.getTicketCode(),
                "type", ticket.getTicketType().name(),
                "assetNumber", asset.getAssetNumber()
        ));

        notifyInvolvedUsers(ticket, asset, "PENDIENTE_APROBACION", principal.fullName());
        return ticket;
    }

    /**
     * Single ACID Transaction executing ticket approval, asset state transition,
     * assignment/movement/maintenance/part/decommission updates, and audit log.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public TicketEntity approveTicket(UUID ticketId, ApproveTicketRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        TicketEntity ticket = ticketRepository.findByIdAndCountryId(ticketId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        if (ticket.getStatus() != TicketStatus.PENDIENTE_APROBACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya fue procesado (estado: " + ticket.getStatus() + ")");
        }

        AssetEntity asset = assetRepository.findByIdAndCountryId(ticket.getAssetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo asociado no encontrado"));

        OffsetDateTime now = OffsetDateTime.now();
        ticket.setApprovedByUserId(principal.userId());
        ticket.setApprovedAt(now);
        ticket.setUpdatedBy(principal.username());

        ApproveTicketRequest body = req != null ? req : new ApproveTicketRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        String destLocation = firstNonBlank(body.destinationLocation(), ticket.getDestinationLocation(), asset.getPhysicalLocation());
        String transporter = firstNonBlank(body.transporterName(), ticket.getTransporterName(), null);
        String transporterDoc = firstNonBlank(body.transporterIdDoc(), ticket.getTransporterIdDoc(), null);
        if (transporter != null) ticket.setTransporterName(transporter);
        if (transporterDoc != null) ticket.setTransporterIdDoc(transporterDoc);
        if (destLocation != null) ticket.setDestinationLocation(destLocation);

        switch (ticket.getTicketType()) {
            case ASIGNACION -> {
                if (ticket.getTargetPersonnelId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La asignación requiere especificar persona, oficina o grupo destino");
                }
                closeOpenAssignmentIfAny(asset.getId(), asset.getStatus(), principal.username(), now);

                AssignmentEntity assignment = new AssignmentEntity();
                assignment.setId(UUID.randomUUID());
                assignment.setCountryId(principal.countryId());
                assignment.setAssetId(asset.getId());
                assignment.setPersonnelId(ticket.getTargetPersonnelId());
                assignment.setTicketId(ticket.getId());
                assignment.setAssignedAt(now);
                assignment.setReason(ticket.getReason());
                assignment.setAssetStatusAtAssign(AssetStatus.ASIGNADO);
                assignment.setNotes(body.comment());
                assignment.setCreatedBy(principal.username());
                assignment.setUpdatedBy(principal.username());
                assignmentRepository.save(assignment);

                asset.setStatus(AssetStatus.ASIGNADO);
                asset.setCurrentAssigneeId(ticket.getTargetPersonnelId());
                if (destLocation != null && !destLocation.isBlank()) {
                    asset.setPhysicalLocation(destLocation);
                }
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
            case DEVOLUCION -> {
                closeOpenAssignmentIfAny(asset.getId(), AssetStatus.DISPONIBLE, principal.username(), now);
                asset.setStatus(AssetStatus.DISPONIBLE);
                asset.setCurrentAssigneeId(null);
                if (destLocation != null && !destLocation.isBlank()) {
                    asset.setPhysicalLocation(destLocation);
                }
                recordMovement(principal, asset, ticket, MovementType.ENTRADA, EntrySource.DEVOLUCION,
                        ticket.getOriginLocation(), asset.getPhysicalLocation(), transporter, transporterDoc, now);
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
            case TRASLADO_TRANSPORTE, SALIDA -> {
                MovementType mType = ticket.getTicketType() == TicketType.SALIDA ? MovementType.SALIDA : MovementType.TRASLADO;
                recordMovement(principal, asset, ticket, mType, null,
                        ticket.getOriginLocation(), destLocation, transporter, transporterDoc, now);
                asset.setStatus(AssetStatus.EN_TRANSPORTE);
                ticket.setStatus(TicketStatus.EN_PROCESO);
            }
            case ENTRADA_INGRESO -> {
                EntrySource source = body.entrySource() != null ? body.entrySource() : extractEntrySource(ticket.getPayloadJson());
                recordMovement(principal, asset, ticket, MovementType.ENTRADA, source,
                        ticket.getOriginLocation(), destLocation, transporter, transporterDoc, now);
                asset.setStatus(AssetStatus.DISPONIBLE);
                asset.setPhysicalLocation(destLocation);
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
            case MANTENIMIENTO -> {
                MaintenanceEntity maint = new MaintenanceEntity();
                maint.setId(UUID.randomUUID());
                maint.setCountryId(principal.countryId());
                maint.setAssetId(asset.getId());
                maint.setTicketId(ticket.getId());
                maint.setMaintenanceMode(body.maintenanceMode() != null ? body.maintenanceMode() : MaintenanceMode.INTERNO);
                maint.setProviderName(body.externalProviderName());
                UUID techId = body.assignedMaintenanceUserId() != null ? body.assignedMaintenanceUserId() : ticket.getAssignedMaintenanceUserId();
                maint.setTechnicianUserId(techId);
                if (techId != null) {
                    ticket.setAssignedMaintenanceUserId(techId);
                    userRepository.findById(techId).ifPresent(u -> maint.setTechnicianName(u.getFullName()));
                }
                maint.setFailureDescription(ticket.getTitle() + ": " + ticket.getReason());
                maint.setReason(ticket.getReason());
                maint.setStatus(MaintenanceStatus.ABIERTA);
                maint.setStartedAt(now);
                maint.setCreatedBy(principal.username());
                maint.setUpdatedBy(principal.username());
                maintenanceRepository.save(maint);

                asset.setStatus(AssetStatus.EN_MANTENIMIENTO);
                ticket.setStatus(TicketStatus.EN_PROCESO);
            }
            case CAMBIO_PARTE -> {
                UUID partId = body.storedPartId() != null ? body.storedPartId() : extractUuid(ticket.getPayloadJson(), "storedPartId");
                String installedDesc = firstNonBlank(body.installedPartDesc(), extractStr(ticket.getPayloadJson(), "installedPartDesc"), "Parte reemplazada");
                String removedDesc = firstNonBlank(body.removedPartDesc(), extractStr(ticket.getPayloadJson(), "removedPartDesc"), "Componente previo");
                applyPartChange(principal, asset.getId(), null, ticket.getId(), partId, removedDesc, installedDesc, ticket.getReason(), now);
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
            case DESINCORPORACION -> {
                closeOpenAssignmentIfAny(asset.getId(), AssetStatus.DESINCORPORADO, principal.username(), now);
                DecommissionDisposition disp = body.disposition() != null ? body.disposition() : extractDisposition(ticket.getPayloadJson());
                String partsNotes = firstNonBlank(body.partsUsageNotes(), extractStr(ticket.getPayloadJson(), "partsUsageNotes"), "Sin partes reutilizadas");
                String recipient = firstNonBlank(body.recipientInfo(), extractStr(ticket.getPayloadJson(), "recipientInfo"), null);

                DecommissioningEntity dec = new DecommissioningEntity();
                dec.setId(UUID.randomUUID());
                dec.setCountryId(principal.countryId());
                dec.setAssetId(asset.getId());
                dec.setTicketId(ticket.getId());
                dec.setDisposition(disp);
                dec.setReason(ticket.getReason());
                dec.setPartsUsageNotes(partsNotes);
                dec.setRecipientInfo(recipient);
                dec.setDecommissionedAt(now);
                dec.setApprovedByUserId(principal.userId());
                dec.setCreatedBy(principal.username());
                dec.setUpdatedBy(principal.username());
                decommissioningRepository.save(dec);

                asset.setStatus(AssetStatus.DESINCORPORADO);
                asset.setCurrentAssigneeId(null);
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
            case CONSIGNACION_VENTA -> {
                String saleMode = extractStr(ticket.getPayloadJson(), "operationMode");
                if ("VENTA".equalsIgnoreCase(saleMode)) {
                    closeOpenAssignmentIfAny(asset.getId(), AssetStatus.DESINCORPORADO, principal.username(), now);
                    asset.setStatus(AssetStatus.DESINCORPORADO);
                    asset.setCurrentAssigneeId(null);
                }
                ticket.setStatus(TicketStatus.COMPLETADO);
            }
        }

        asset.setUpdatedBy(principal.username());
        assetRepository.save(asset);
        ticket = ticketRepository.save(ticket);

        recordApprovalStep(ticket.getId(), principal.countryId(), principal.userId(), "APROBADO", body.comment());
        auditService.record("TICKET_APPROVE", "TICKET", ticket.getId().toString(), Map.of(
                "ticketCode", ticket.getTicketCode(),
                "ticketType", ticket.getTicketType().name(),
                "resultingTicketStatus", ticket.getStatus().name(),
                "resultingAssetStatus", asset.getStatus().name()
        ));

        notifyInvolvedUsers(ticket, asset, ticket.getStatus().name(), principal.fullName());
        return ticket;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public TicketEntity rejectTicket(UUID ticketId, RejectTicketRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        TicketEntity ticket = ticketRepository.findByIdAndCountryId(ticketId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        if (ticket.getStatus() != TicketStatus.PENDIENTE_APROBACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden rechazar tickets pendientes");
        }
        AssetEntity asset = assetRepository.findByIdAndCountryId(ticket.getAssetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        ticket.setStatus(TicketStatus.RECHAZADO);
        ticket.setRejectionReason(req.rejectionReason().trim());
        ticket.setApprovedByUserId(principal.userId());
        ticket.setApprovedAt(OffsetDateTime.now());
        ticket.setUpdatedBy(principal.username());
        ticket = ticketRepository.save(ticket);

        recordApprovalStep(ticket.getId(), principal.countryId(), principal.userId(), "RECHAZADO", req.rejectionReason());
        auditService.record("TICKET_REJECT", "TICKET", ticket.getId().toString(), Map.of(
                "ticketCode", ticket.getTicketCode(),
                "reason", req.rejectionReason()
        ));

        notifyInvolvedUsers(ticket, asset, "RECHAZADO", principal.fullName());
        return ticket;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','MAINTENANCE')")
    public MaintenanceEntity completeMaintenance(UUID maintenanceId, CompleteMaintenanceRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        MaintenanceEntity maint = maintenanceRepository.findByIdAndCountryId(maintenanceId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de mantenimiento no encontrada"));
        if (maint.getStatus() == MaintenanceStatus.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El mantenimiento ya fue finalizado");
        }
        AssetEntity asset = assetRepository.findByIdAndCountryId(maint.getAssetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        OffsetDateTime now = OffsetDateTime.now();
        maint.setWorkPerformed(req.workPerformed().trim());
        maint.setStatus(MaintenanceStatus.FINALIZADA);
        maint.setCompletedAt(now);
        if (maint.getTechnicianUserId() == null) {
            maint.setTechnicianUserId(principal.userId());
            maint.setTechnicianName(principal.fullName());
        }
        maint.setUpdatedBy(principal.username());
        maint = maintenanceRepository.save(maint);

        if (req.storedPartId() != null || (req.installedPartDesc() != null && !req.installedPartDesc().isBlank())) {
            applyPartChange(
                    principal, asset.getId(), maint.getId(), maint.getTicketId(),
                    req.storedPartId(),
                    req.removedPartDesc(),
                    req.installedPartDesc() != null ? req.installedPartDesc() : "Parte de almacén",
                    firstNonBlank(req.partChangeReason(), req.workPerformed(), "Cambio durante mantenimiento"),
                    now
            );
        }

        asset.setStatus(req.resultingAssetStatus());
        asset.setUpdatedBy(principal.username());
        assetRepository.save(asset);

        if (maint.getTicketId() != null) {
            ticketRepository.findByIdAndCountryId(maint.getTicketId(), principal.countryId()).ifPresent(t -> {
                t.setStatus(TicketStatus.COMPLETADO);
                t.setUpdatedBy(principal.username());
                ticketRepository.save(t);
                recordApprovalStep(t.getId(), principal.countryId(), principal.userId(), "COMPLETADO", req.workPerformed());
                notifyInvolvedUsers(t, asset, "COMPLETADO", principal.fullName());
            });
        }

        auditService.record("MAINTENANCE_COMPLETE", "MAINTENANCE", maint.getId().toString(), Map.of(
                "assetNumber", asset.getAssetNumber(),
                "resultingStatus", req.resultingAssetStatus().name()
        ));
        return maint;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','MAINTENANCE')")
    public TicketEntity completeTransport(UUID ticketId, String comment) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        TicketEntity ticket = ticketRepository.findByIdAndCountryId(ticketId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
        AssetEntity asset = assetRepository.findByIdAndCountryId(ticket.getAssetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        if (ticket.getDestinationLocation() != null && !ticket.getDestinationLocation().isBlank()) {
            asset.setPhysicalLocation(ticket.getDestinationLocation());
        }
        asset.setStatus(asset.getCurrentAssigneeId() != null ? AssetStatus.ASIGNADO : AssetStatus.DISPONIBLE);
        asset.setUpdatedBy(principal.username());
        assetRepository.save(asset);

        ticket.setStatus(TicketStatus.COMPLETADO);
        ticket.setUpdatedBy(principal.username());
        ticket = ticketRepository.save(ticket);

        recordApprovalStep(ticket.getId(), principal.countryId(), principal.userId(), "COMPLETADO",
                firstNonBlank(comment, "Recepción en destino confirmada", "Completado"));
        auditService.record("TRANSPORT_COMPLETE", "TICKET", ticket.getId().toString(), Map.of(
                "assetNumber", asset.getAssetNumber(),
                "destination", asset.getPhysicalLocation()
        ));
        notifyInvolvedUsers(ticket, asset, "COMPLETADO", principal.fullName());
        return ticket;
    }

    @Transactional
    public GeneratedDocumentEntity generateActa(GenerateActaRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = assetRepository.findByIdAndCountryId(req.assetId(), principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        TicketEntity ticket = req.ticketId() != null
                ? ticketRepository.findByIdAndCountryId(req.ticketId(), principal.countryId()).orElse(null)
                : null;

        String approverName = "N/A";
        if (ticket != null && ticket.getApprovedByUserId() != null) {
            approverName = userRepository.findById(ticket.getApprovedByUserId()).map(UserEntity::getFullName).orElse("Supervisor");
        }
        String assigneeDisplay = "No asignado";
        if (asset.getCurrentAssigneeId() != null) {
            assigneeDisplay = personnelRepository.findByIdAndCountryId(asset.getCurrentAssigneeId(), principal.countryId())
                    .map(PersonnelEntity::displayLabel).orElse("Personal");
        }
        String transporterDisplay = ticket != null && ticket.getTransporterName() != null
                ? ticket.getTransporterName() + (ticket.getTransporterIdDoc() != null ? " (" + ticket.getTransporterIdDoc() + ")" : "")
                : "N/A";
        String maintDisplay = req.documentType() == DocumentType.REPARACION
                ? maintenanceRepository.findByAssetIdOrderByStartedAtDesc(asset.getId()).stream().findFirst()
                    .map(m -> m.getMaintenanceMode() + " — " + firstNonBlank(m.getWorkPerformed(), m.getFailureDescription(), "En curso"))
                    .orElse("Reparación registrada")
                : "N/A";

        String docNumber = "ACT-" + req.documentType().name().substring(0, 4) + "-" +
                principal.countryCode() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        var pdfRes = satelliteClient.renderActaPdf(new SatelliteClient.RenderActaReq(
                docNumber,
                req.documentType().name(),
                principal.countryCode(),
                asset.getId().toString(),
                asset.getAssetNumber(),
                asset.getEquipmentNumber(),
                asset.getCategory().name(),
                asset.getBrand(),
                asset.getModel(),
                asset.getSerialNumber(),
                asset.getConditionStatus().name(),
                asset.getPhysicalLocation(),
                ticket != null ? ticket.getTicketCode() : "SIN-TICKET",
                req.reason(),
                principal.fullName(),
                approverName,
                assigneeDisplay,
                ticket != null ? ticket.getOriginLocation() : asset.getPhysicalLocation(),
                ticket != null ? ticket.getDestinationLocation() : asset.getPhysicalLocation(),
                transporterDisplay,
                maintDisplay,
                firstNonBlank(req.notes(), asset.getObservations(), "Acta oficial generada desde GlobalR Inventario"),
                OffsetDateTime.now().toString()
        ));

        GeneratedDocumentEntity doc = new GeneratedDocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setCountryId(principal.countryId());
        doc.setAssetId(asset.getId());
        doc.setTicketId(ticket != null ? ticket.getId() : null);
        doc.setDocumentType(req.documentType());
        doc.setDocumentNumber(docNumber);
        doc.setReason(req.reason());
        doc.setMinioObjectKey(pdfRes.objectKey());
        doc.setSha256Hash(pdfRes.sha256Hash());
        doc.setGeneratedByUserId(principal.userId());
        doc.setGeneratedAt(OffsetDateTime.now());
        doc.setCreatedBy(principal.username());
        doc.setUpdatedBy(principal.username());
        doc = documentRepository.save(doc);

        auditService.record("PDF_GENERATE", "DOCUMENT", doc.getId().toString(), Map.of(
                "documentNumber", docNumber,
                "documentType", req.documentType().name(),
                "assetNumber", asset.getAssetNumber(),
                "processingMs", pdfRes.processingMs()
        ));
        return doc;
    }

    private void closeOpenAssignmentIfAny(UUID assetId, AssetStatus returnStatus, String actor, OffsetDateTime now) {
        assignmentRepository.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(assetId).ifPresent(open -> {
            open.setReturnedAt(now);
            open.setAssetStatusAtReturn(returnStatus);
            open.setUpdatedBy(actor);
            assignmentRepository.save(open);
        });
    }

    private void recordMovement(TenantContext.PrincipalInfo principal, AssetEntity asset, TicketEntity ticket,
                                MovementType type, EntrySource entrySource, String origin, String dest,
                                String transporter, String transporterDoc, OffsetDateTime now) {
        MovementTransportEntity mv = new MovementTransportEntity();
        mv.setId(UUID.randomUUID());
        mv.setCountryId(principal.countryId());
        mv.setAssetId(asset.getId());
        mv.setTicketId(ticket.getId());
        mv.setMovementType(type);
        mv.setEntrySource(entrySource);
        mv.setOriginLocation(origin);
        mv.setDestinationLocation(firstNonBlank(dest, asset.getPhysicalLocation(), "Oficina Principal"));
        mv.setTransporterName(transporter);
        mv.setTransporterIdDoc(transporterDoc);
        mv.setExecutedByUserId(principal.userId());
        mv.setReason(ticket.getReason());
        mv.setMovedAt(now);
        mv.setCreatedBy(principal.username());
        mv.setUpdatedBy(principal.username());
        movementRepository.save(mv);
    }

    private void applyPartChange(TenantContext.PrincipalInfo principal, UUID assetId, UUID maintenanceId, UUID ticketId,
                                 UUID storedPartId, String removedDesc, String installedDesc, String reason, OffsetDateTime now) {
        String resolvedInstalled = installedDesc;
        if (storedPartId != null) {
            StoredPartEntity part = storedPartRepository.findByIdAndCountryId(storedPartId, principal.countryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parte de almacén no encontrada"));
            if (part.getStatus() != StoredPartStatus.EN_ALMACEN) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La parte seleccionada no está disponible en almacén");
            }
            part.setStatus(StoredPartStatus.INSTALADA);
            part.setInstalledInAssetId(assetId);
            part.setUpdatedBy(principal.username());
            storedPartRepository.save(part);
            resolvedInstalled = part.getPartType() + " " + part.getCapacity() + " " + part.getBrand() + " (S/N: " + part.getSerialNumber() + ")";
        }

        PartChangeEntity pc = new PartChangeEntity();
        pc.setId(UUID.randomUUID());
        pc.setCountryId(principal.countryId());
        pc.setAssetId(assetId);
        pc.setMaintenanceId(maintenanceId);
        pc.setTicketId(ticketId);
        pc.setStoredPartId(storedPartId);
        pc.setRemovedPartDesc(removedDesc);
        pc.setInstalledPartDesc(resolvedInstalled);
        pc.setReason(reason);
        pc.setChangedAt(now);
        pc.setPerformedByUserId(principal.userId());
        pc.setCreatedBy(principal.username());
        pc.setUpdatedBy(principal.username());
        partChangeRepository.save(pc);
    }

    private void recordApprovalStep(UUID ticketId, UUID countryId, UUID actorId, String action, String comment) {
        TicketApprovalEntity step = new TicketApprovalEntity();
        step.setId(UUID.randomUUID());
        step.setCountryId(countryId);
        step.setTicketId(ticketId);
        step.setActorUserId(actorId);
        step.setAction(action);
        step.setComment(comment);
        approvalRepository.save(step);
    }

    private void notifyInvolvedUsers(TicketEntity ticket, AssetEntity asset, String statusLabel, String actorName) {
        List<String> recipients = new ArrayList<>();
        userRepository.findById(ticket.getRequesterUserId()).ifPresent(u -> recipients.add(u.getEmail()));
        userRepository.findByCountryIdAndRoleIn(ticket.getCountryId(), List.of(Role.SUPERVISOR, Role.ADMIN))
                .forEach(u -> recipients.add(u.getEmail()));
        if (ticket.getTicketType() == TicketType.MANTENIMIENTO || ticket.getAssignedMaintenanceUserId() != null) {
            userRepository.findByCountryIdAndRoleIn(ticket.getCountryId(), List.of(Role.MAINTENANCE))
                    .forEach(u -> recipients.add(u.getEmail()));
        }
        TenantContext.PrincipalInfo p = TenantContext.get();
        String cCode = p != null ? p.countryCode() : "VE";

        satelliteClient.notifyTicketEventAsync(new SatelliteClient.NotifyTicketReq(
                ticket.getTicketCode(),
                ticket.getTicketType().name(),
                statusLabel,
                cCode,
                asset.getAssetNumber(),
                asset.getBrand() + " " + asset.getModel(),
                ticket.getReason(),
                actorName,
                recipients.stream().distinct().toList()
        ));
    }

    private EntrySource extractEntrySource(Map<String, Object> payload) {
        String raw = extractStr(payload, "entrySource");
        if (raw == null) return EntrySource.COMPRA;
        try { return EntrySource.valueOf(raw.toUpperCase()); } catch (Exception e) { return EntrySource.COMPRA; }
    }

    private DecommissionDisposition extractDisposition(Map<String, Object> payload) {
        String raw = extractStr(payload, "disposition");
        if (raw == null) return DecommissionDisposition.DESECHADO;
        try { return DecommissionDisposition.valueOf(raw.toUpperCase()); } catch (Exception e) { return DecommissionDisposition.DESECHADO; }
    }

    private String extractStr(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object val = payload.get(key);
        return val != null ? val.toString() : null;
    }

    private UUID extractUuid(Map<String, Object> payload, String key) {
        String raw = extractStr(payload, key);
        if (raw == null || raw.isBlank()) return null;
        try { return UUID.fromString(raw); } catch (Exception e) { return null; }
    }

    private String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return fallback;
    }

    private TenantContext.PrincipalInfo requirePrincipal() {
        TenantContext.PrincipalInfo p = TenantContext.get();
        if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida");
        return p;
    }
}
