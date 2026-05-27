package com.vanthucac.audit.repository;

import com.vanthucac.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId, Pageable pageable);

    Page<AuditLog> findByActorId(Long actorId, Pageable pageable);
}