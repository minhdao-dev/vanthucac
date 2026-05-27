package com.vanthucac.seller.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_transaction_reference",
                        columnNames = {"wallet_id", "transaction_type", "reference_type", "reference_id"}
                )
        },
        indexes = {
                @Index(name = "idx_wallet_transactions_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wallet_transactions_reference", columnList = "reference_type, reference_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private SellerWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(length = 500)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum TransactionType {
        ESCROW_RELEASE,
        REFUND,
        WITHDRAWAL,
        ADJUSTMENT
    }

    public static WalletTransaction create(
            SellerWallet wallet,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceType,
            Long referenceId,
            String description
    ) {
        var transaction = new WalletTransaction();
        transaction.wallet = wallet;
        transaction.type = type;
        transaction.amount = amount;
        transaction.balanceBefore = balanceBefore;
        transaction.balanceAfter = balanceAfter;
        transaction.referenceType = referenceType;
        transaction.referenceId = referenceId;
        transaction.description = description;
        return transaction;
    }
}