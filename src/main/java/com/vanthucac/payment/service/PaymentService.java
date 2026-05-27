package com.vanthucac.payment.service;

import com.vanthucac.common.outbox.OutboxEventService;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.payment.dto.PaymentResponse;
import com.vanthucac.payment.entity.EscrowRecord;
import com.vanthucac.payment.entity.Payment;
import com.vanthucac.payment.exception.PaymentException;
import com.vanthucac.payment.provider.PaymentIntentRequest;
import com.vanthucac.payment.provider.PaymentProvider;
import com.vanthucac.payment.repository.EscrowRecordRepository;
import com.vanthucac.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final EscrowRecordRepository escrowRecordRepository;
    private final PaymentProvider paymentProvider;
    private final OutboxEventService outboxEventService;

    public PaymentService(
            PaymentRepository paymentRepository,
            EscrowRecordRepository escrowRecordRepository,
            PaymentProvider paymentProvider,
            OutboxEventService outboxEventService
    ) {
        this.paymentRepository = paymentRepository;
        this.escrowRecordRepository = escrowRecordRepository;
        this.paymentProvider = paymentProvider;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public void createPaymentIntent(Order order, BigDecimal amount) {
        var request = new PaymentIntentRequest(
                order.getId(),
                amount,
                Payment.PaymentMethod.MOCK
        );
        var intent = paymentProvider.createPaymentIntent(request);
        var payment = Payment.createPending(
                order,
                amount,
                intent.paymentMethod(),
                intent.providerPaymentId(),
                intent.checkoutUrl()
        );
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(PaymentException::paymentNotFound);

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw PaymentException.accessDenied();
        }

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse completeMockPayment(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(PaymentException::paymentNotFound);

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw PaymentException.accessDenied();
        }

        if (payment.isCompleted()) {
            return PaymentResponse.from(payment);
        }

        if (payment.getPaymentMethod() != Payment.PaymentMethod.MOCK || !payment.isPayable()) {
            throw PaymentException.paymentNotPayable();
        }

        payment.markProcessing();
        var verification = paymentProvider.verifyPayment(payment.getProviderPaymentId());
        if (!verification.success()) {
            payment.fail();
            throw PaymentException.providerRejected();
        }

        payment.complete();
        createEscrowRecordsForPaidOrder(payment.getOrder());
        publishPaymentCompletedEvent(payment);

        log.info("Payment {} completed for order {}", payment.getId(), payment.getOrder().getId());

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public void ensureOrderPaymentCompleted(Long orderId) {
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(PaymentException::paymentNotFound);
        if (!payment.isCompleted()) {
            throw PaymentException.paymentNotCompleted();
        }
    }

    @Transactional
    public void cancelPaymentIfUnpaid(Long orderId) {
        paymentRepository.findByOrderId(orderId)
                .filter(payment -> !payment.isCompleted())
                .ifPresent(Payment::cancel);
    }

    private void createEscrowRecordsForPaidOrder(Order parentOrder) {
        if (!parentOrder.isParentOrder()) {
            throw OrderException.invalidStatusTransition();
        }

        parentOrder.getSubOrders().stream()
                .filter(subOrder -> subOrder.getOrderType() == Order.OrderType.C2C)
                .filter(subOrder -> subOrder.getSeller() != null)
                .filter(subOrder -> escrowRecordRepository.findByOrderId(subOrder.getId()).isEmpty())
                .forEach(subOrder -> {
                    var escrow = EscrowRecord.create(subOrder, subOrder.getTotalAmount());
                    escrowRecordRepository.save(escrow);
                    publishEscrowCreatedEvent(escrow);
                    log.info("Escrow created for sub-order {} after payment completion", subOrder.getId());
                });
    }

    private void publishPaymentCompletedEvent(Payment payment) {
        outboxEventService.publish(
                "PAYMENT_COMPLETED",
                "PAYMENT",
                payment.getId(),
                Map.of(
                        "paymentId", payment.getId(),
                        "orderId", payment.getOrder().getId(),
                        "userId", payment.getOrder().getUser().getId(),
                        "amount", payment.getAmount(),
                        "paymentMethod", payment.getPaymentMethod().name(),
                        "providerPaymentId", payment.getProviderPaymentId()
                )
        );
    }

    private void publishEscrowCreatedEvent(EscrowRecord escrow) {
        outboxEventService.publish(
                "ESCROW_CREATED",
                "ESCROW",
                escrow.getId(),
                Map.of(
                        "escrowId", escrow.getId(),
                        "orderId", escrow.getOrder().getId(),
                        "sellerId", escrow.getOrder().getSeller().getId(),
                        "amount", escrow.getAmount()
                )
        );
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}