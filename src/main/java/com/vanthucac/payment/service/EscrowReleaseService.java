package com.vanthucac.payment.service;

import com.vanthucac.common.config.CommissionProperties;
import com.vanthucac.common.outbox.OutboxEventService;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.payment.entity.PlatformCommission;
import com.vanthucac.payment.repository.EscrowRecordRepository;
import com.vanthucac.payment.repository.PlatformCommissionRepository;
import com.vanthucac.seller.entity.WalletTransaction;
import com.vanthucac.seller.exception.SellerException;
import com.vanthucac.seller.repository.SellerWalletRepository;
import com.vanthucac.seller.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Map;

@Service
public class EscrowReleaseService {

    private static final Logger log = LoggerFactory.getLogger(EscrowReleaseService.class);
    private static final String REFERENCE_ORDER = "ORDER";

    private final EscrowRecordRepository escrowRecordRepository;
    private final PlatformCommissionRepository platformCommissionRepository;
    private final SellerWalletRepository sellerWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CommissionProperties commissionProperties;
    private final OutboxEventService outboxEventService;

    public EscrowReleaseService(
            EscrowRecordRepository escrowRecordRepository,
            PlatformCommissionRepository platformCommissionRepository,
            SellerWalletRepository sellerWalletRepository,
            WalletTransactionRepository walletTransactionRepository,
            CommissionProperties commissionProperties,
            OutboxEventService outboxEventService
    ) {
        this.escrowRecordRepository = escrowRecordRepository;
        this.platformCommissionRepository = platformCommissionRepository;
        this.sellerWalletRepository = sellerWalletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.commissionProperties = commissionProperties;
        this.outboxEventService = outboxEventService;
    }

    public void release(Order subOrder) {
        var escrow = escrowRecordRepository.findByOrderId(subOrder.getId())
                .orElseThrow(() -> {
                    log.error("Escrow not found for sub-order {}", subOrder.getId());
                    return OrderException.orderNotFound();
                });

        if (escrow.isReleased()) {
            log.info("Escrow already released for sub-order {}", subOrder.getId());
            return;
        }

        if (escrow.isNotHolding()) {
            throw OrderException.invalidStatusTransition();
        }

        var seller = subOrder.getSeller();
        var totalAmount = subOrder.getTotalAmount();
        var commissionRate = commissionProperties.rate();
        var commissionAmount = totalAmount
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);
        var netAmount = totalAmount.subtract(commissionAmount);

        if (!platformCommissionRepository.existsByOrderId(subOrder.getId())) {
            var commission = PlatformCommission.create(subOrder, seller, commissionAmount, commissionRate);
            platformCommissionRepository.save(commission);
        }

        var wallet = sellerWalletRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> {
                    log.error("Wallet not found for seller {}", seller.getId());
                    return SellerException.walletNotFound();
                });

        var transactionExists = walletTransactionRepository
                .existsByWalletIdAndTypeAndReferenceTypeAndReferenceId(
                        wallet.getId(),
                        WalletTransaction.TransactionType.ESCROW_RELEASE,
                        REFERENCE_ORDER,
                        subOrder.getId()
                );

        if (transactionExists) {
            throw OrderException.invalidStatusTransition();
        }

        var balanceBefore = wallet.getBalance();
        wallet.credit(netAmount);
        var balanceAfter = wallet.getBalance();

        walletTransactionRepository.save(WalletTransaction.create(
                wallet,
                WalletTransaction.TransactionType.ESCROW_RELEASE,
                netAmount,
                balanceBefore,
                balanceAfter,
                REFERENCE_ORDER,
                subOrder.getId(),
                "Escrow released for order " + subOrder.getId()
        ));

        escrow.release();

        outboxEventService.publish(
                "ESCROW_RELEASED",
                "ORDER",
                subOrder.getId(),
                Map.of(
                        "orderId", subOrder.getId(),
                        "parentOrderId", subOrder.getParentOrder().getId(),
                        "sellerId", seller.getId(),
                        "totalAmount", totalAmount,
                        "commissionAmount", commissionAmount,
                        "netAmount", netAmount
                )
        );

        log.info("Escrow released for sub-order {} — total: {}, commission: {}, net: {}",
                subOrder.getId(), totalAmount, commissionAmount, netAmount);
    }

    public void refund(Order subOrder) {
        escrowRecordRepository.findByOrderId(subOrder.getId())
                .ifPresent(escrow -> {
                    if (escrow.isNotHolding()) {
                        throw OrderException.invalidStatusTransition();
                    }

                    escrow.refund();

                    outboxEventService.publish(
                            "ESCROW_REFUNDED",
                            "ORDER",
                            subOrder.getId(),
                            Map.of(
                                    "orderId", subOrder.getId(),
                                    "parentOrderId", subOrder.getParentOrder().getId(),
                                    "sellerId", subOrder.getSeller().getId(),
                                    "amount", subOrder.getTotalAmount()
                            )
                    );

                    log.info("Escrow refunded for sub-order {}", subOrder.getId());
                });
    }
}