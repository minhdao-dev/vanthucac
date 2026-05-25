package com.vanthucac.payment.repository;

import com.vanthucac.payment.entity.PlatformCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformCommissionRepository extends JpaRepository<PlatformCommission, Long> {
}