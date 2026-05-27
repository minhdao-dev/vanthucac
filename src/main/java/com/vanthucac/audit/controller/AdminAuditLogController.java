package com.vanthucac.audit.controller;

import com.vanthucac.audit.dto.AuditLogResponse;
import com.vanthucac.audit.service.AuditLogService;
import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.common.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var response = auditLogService.getAuditLogs(
                action,
                resourceType,
                resourceId,
                actorId,
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully", response));
    }
}