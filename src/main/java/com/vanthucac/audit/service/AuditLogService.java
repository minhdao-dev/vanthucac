package com.vanthucac.audit.service;

import com.vanthucac.audit.dto.AuditLogResponse;
import com.vanthucac.audit.entity.AuditLog;
import com.vanthucac.audit.repository.AuditLogRepository;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String ROLE_CLAIM = "roles";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository
    ) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(
            Jwt jwt,
            String action,
            String resourceType,
            Long resourceId,
            String description
    ) {
        var actorId = extractUserId(jwt);
        var user = actorId == null
                ? null
                : userRepository.findById(actorId).orElse(null);

        var request = currentRequest();

        var auditLog = AuditLog.create(
                actorId,
                user == null ? null : user.getEmail(),
                extractActorRole(jwt),
                action,
                resourceType,
                resourceId,
                description,
                extractClientIp(request),
                extractUserAgent(request)
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logSystem(
            String action,
            String resourceType,
            Long resourceId,
            String description
    ) {
        var request = currentRequest();

        var auditLog = AuditLog.create(
                null,
                null,
                SYSTEM_ACTOR,
                action,
                resourceType,
                resourceId,
                description,
                extractClientIp(request),
                extractUserAgent(request)
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(
            String action,
            String resourceType,
            Long resourceId,
            Long actorId,
            int page,
            int size
    ) {
        var pageable = PageableUtils.build(page, size, "createdAt,desc");

        if (action != null && !action.isBlank()) {
            return PageResponse.from(auditLogRepository.findByAction(action, pageable)
                    .map(AuditLogResponse::from));
        }

        if (resourceType != null && !resourceType.isBlank() && resourceId != null) {
            return PageResponse.from(auditLogRepository
                    .findByResourceTypeAndResourceId(resourceType, resourceId, pageable)
                    .map(AuditLogResponse::from));
        }

        if (actorId != null) {
            return PageResponse.from(auditLogRepository.findByActorId(actorId, pageable)
                    .map(AuditLogResponse::from));
        }

        return PageResponse.from(auditLogRepository.findAll(pageable)
                .map(AuditLogResponse::from));
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return null;
        }
        return Long.parseLong(jwt.getSubject());
    }

    private String extractActorRole(Jwt jwt) {
        if (jwt == null) {
            return SYSTEM_ACTOR;
        }

        var roles = jwt.getClaimAsStringList(ROLE_CLAIM);
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return String.join(",", roles);
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        var forwarded = headerValue(request, "X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }

        var realIp = headerValue(request, "X-Real-IP");
        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return headerValue(request, "User-Agent");
    }

    private String headerValue(HttpServletRequest request, String headerName) {
        var value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}