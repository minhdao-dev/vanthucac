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
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED, PARTIALLY_REFUNDED
    }

    public enum PaymentMethod {
        MOCK, BANK_TRANSFER, VNPAY, MOMO, STRIPE
    }

    public static Payment createPending(Order order, BigDecimal amount,
                                        PaymentMethod paymentMethod,
                                        String providerPaymentId,
                                        String checkoutUrl) {
        var payment = new Payment();
        payment.order = order;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        payment.paymentMethod = paymentMethod;
        payment.providerPaymentId = providerPaymentId;
        payment.checkoutUrl = checkoutUrl;
        return payment;
    }

    public void markProcessing() {
        if (status == PaymentStatus.PENDING) {
            this.status = PaymentStatus.PROCESSING;
        }
    }

    public void complete() {
        if (status == PaymentStatus.COMPLETED) {
            return;
        }
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = Instant.now();
        this.failedAt = null;
    }

    public void fail() {
        if (status == PaymentStatus.COMPLETED) {
            return;
        }
        this.status = PaymentStatus.FAILED;
        this.failedAt = Instant.now();
    }

    public void cancel() {
        if (status == PaymentStatus.COMPLETED) {
            return;
        }
        this.status = PaymentStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isPayable() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    public boolean isMockPayment() {
        return paymentMethod == PaymentMethod.MOCK;
    }
}