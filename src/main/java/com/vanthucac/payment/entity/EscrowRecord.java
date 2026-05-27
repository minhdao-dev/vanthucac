package com.vanthucac.payment.entity;

import com.vanthucac.order.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "escrow_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EscrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscrowStatus status = EscrowStatus.HOLDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum EscrowStatus {
        HOLDING, RELEASING, RELEASED, REFUNDED
    }

    public static EscrowRecord create(Order subOrder, BigDecimal amount) {
        var escrow = new EscrowRecord();
        escrow.order = subOrder;
        escrow.amount = amount;
        return escrow;
    }

    public void release() {
        if (isReleased()) {
            return;
        }
        if (isNotHolding()) {
            throw new IllegalStateException("Escrow cannot be released from status " + status);
        }
        this.status = EscrowStatus.RELEASED;
    }

    public void refund() {
        if (isRefunded()) {
            return;
        }
        if (isNotHolding()) {
            throw new IllegalStateException("Escrow cannot be refunded from status " + status);
        }
        this.status = EscrowStatus.REFUNDED;
    }

    public boolean isNotHolding() {
        return status != EscrowStatus.HOLDING;
    }

    public boolean isReleased() {
        return status == EscrowStatus.RELEASED;
    }

    public boolean isRefunded() {
        return status == EscrowStatus.REFUNDED;
    }
}