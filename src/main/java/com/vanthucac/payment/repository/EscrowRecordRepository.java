package com.vanthucac.payment.repository;

import com.vanthucac.payment.entity.EscrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscrowRecordRepository extends JpaRepository<EscrowRecord, Long> {
    Optional<EscrowRecord> findByOrderId(Long orderId);
}