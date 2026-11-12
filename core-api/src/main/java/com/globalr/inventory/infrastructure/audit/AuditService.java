package com.globalr.inventory.infrastructure.audit;

import com.globalr.inventory.domain.model.AuditLogEntity;
import com.globalr.inventory.domain.repository.AuditLogRepository;
import com.globalr.inventory.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String action, String entityType, String entityId, Map<String, Object> details) {
        TenantContext.PrincipalInfo p = TenantContext.get();
        AuditLogEntity log = new AuditLogEntity();
        log.setCountryId(p != null ? p.countryId() : null);
        log.setActorUserId(p != null ? p.userId() : null);
        log.setActorUsername(p != null ? p.username() : "system");
        log.setActorRole(p != null && p.role() != null ? p.role().name() : "SYSTEM");
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setSourceChannel(p != null ? p.sourceChannel() : "SYSTEM");
        log.setClientIp(p != null ? p.clientIp() : "127.0.0.1");
        log.setDetailsJson(details != null ? details : Map.of());
        auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthEvent(UUID countryId, UUID userId, String username, String role, String action, Map<String, Object> details) {
        AuditLogEntity log = new AuditLogEntity();
        log.setCountryId(countryId);
        log.setActorUserId(userId);
        log.setActorUsername(username != null ? username : "anonymous");
        log.setActorRole(role != null ? role : "UNKNOWN");
        log.setAction(action);
        log.setEntityType("AUTH");
        log.setEntityId(userId != null ? userId.toString() : username);
        log.setSourceChannel("WEB");
        log.setDetailsJson(details != null ? details : Map.of());
        auditLogRepository.save(log);
    }
}
