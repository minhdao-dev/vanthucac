package com.vanthucac.audit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_actor_id", columnList = "actor_id"),
                @Index(name = "idx_audit_logs_action", columnList = "action"),
                @Index(name = "idx_audit_logs_resource", columnList = "resource_type, resource_id"),
                @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(length = 1000)
    private String description;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AuditLog create(
            Long actorId,
            String actorEmail,
            String actorRole,
            String action,
            String resourceType,
            Long resourceId,
            String description,
            String ipAddress,
            String userAgent
    ) {
        var auditLog = new AuditLog();
        auditLog.actorId = actorId;
        auditLog.actorEmail = actorEmail;
        auditLog.actorRole = actorRole;
        auditLog.action = action;
        auditLog.resourceType = resourceType;
        auditLog.resourceId = resourceId;
        auditLog.description = description;
        auditLog.ipAddress = ipAddress;
        auditLog.userAgent = userAgent;
        return auditLog;
    }
}