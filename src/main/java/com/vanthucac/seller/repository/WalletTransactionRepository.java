package com.vanthucac.seller.repository;

import com.vanthucac.seller.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    boolean existsByWalletIdAndTypeAndReferenceTypeAndReferenceId(
            Long walletId,
            WalletTransaction.TransactionType type,
            String referenceType,
            Long referenceId
    );
}