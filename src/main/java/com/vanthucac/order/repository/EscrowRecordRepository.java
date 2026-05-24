package com.vanthucac.order.repository;

import com.vanthucac.order.entity.EscrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscrowRecordRepository extends JpaRepository<EscrowRecord, Long> {
}