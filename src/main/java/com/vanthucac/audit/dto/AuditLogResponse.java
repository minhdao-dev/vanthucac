package com.vanthucac.audit.dto;

import com.vanthucac.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        String actorRole,
        String action,
        String resourceType,
        Long resourceId,
        String description,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getActorEmail(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getDescription(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}