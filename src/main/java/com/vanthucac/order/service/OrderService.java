package com.vanthucac.order.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.common.config.CommissionProperties;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.dto.OrderItemResponse;
import com.vanthucac.order.dto.OrderResponse;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.entity.OrderItem;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.order.repository.OrderRepository;
import com.vanthucac.payment.entity.PlatformCommission;
import com.vanthucac.payment.repository.EscrowRecordRepository;
import com.vanthucac.payment.repository.PlatformCommissionRepository;
import com.vanthucac.payment.service.PaymentService;
import com.vanthucac.seller.entity.SellerProfile;
import com.vanthucac.seller.repository.SellerWalletRepository;
import com.vanthucac.user.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final EscrowRecordRepository escrowRecordRepository;
    private final PlatformCommissionRepository platformCommissionRepository;
    private final SellerWalletRepository sellerWalletRepository;
    private final CartRepository cartRepository;
    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserRepository userRepository;
    private final CommissionProperties commissionProperties;

    public OrderService(
            OrderRepository orderRepository,
            PaymentService paymentService,
            EscrowRecordRepository escrowRecordRepository,
            PlatformCommissionRepository platformCommissionRepository,
            SellerWalletRepository sellerWalletRepository,
            CartRepository cartRepository,
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            UserRepository userRepository,
            CommissionProperties commissionProperties
    ) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.escrowRecordRepository = escrowRecordRepository;
        this.platformCommissionRepository = platformCommissionRepository;
        this.sellerWalletRepository = sellerWalletRepository;
        this.cartRepository = cartRepository;
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.userRepository = userRepository;
        this.commissionProperties = commissionProperties;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);
        var user = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(CartException::cartNotFound);

        if (cart.getItems().isEmpty()) {
            throw OrderException.cartEmpty();
        }

        var sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparingLong(item -> item.getListing().getId()))
                .toList();

        var lockedListings = new ArrayList<LockedItem>();
        for (var cartItem : sortedItems) {
            var listing = bookListingRepository
                    .findByIdWithLock(cartItem.getListing().getId())
                    .orElseThrow(() -> OrderException.stockInsufficient(
                            cartItem.getListing().getBookCatalog().getTitle()));

            if (listing.getStatus() != BookListing.ListingStatus.ACTIVE) {
                throw OrderException.stockInsufficient(listing.getBookCatalog().getTitle());
            }

            if (listing.getStock() < cartItem.getQuantity()) {
                throw OrderException.stockInsufficient(listing.getBookCatalog().getTitle());
            }

            listing.deductStock(cartItem.getQuantity());
            lockedListings.add(new LockedItem(cartItem, listing));
        }

        var grandTotal = lockedListings.stream()
                .map(li -> li.listing().getPrice()
                        .multiply(BigDecimal.valueOf(li.cartItem().getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var hasC2C = lockedListings.stream()
                .anyMatch(li -> li.listing().getListingType() == BookListing.ListingType.C2C);
        var parentOrderType = hasC2C ? Order.OrderType.C2C : Order.OrderType.B2C;

        var parentOrder = Order.createParent(user, grandTotal, parentOrderType,
                request.shippingAddress());
        orderRepository.save(parentOrder);

        Map<Optional<SellerProfile>, List<LockedItem>> itemsBySeller = lockedListings.stream()
                .collect(Collectors.groupingBy(
                        li -> Optional.ofNullable(li.listing().getSeller())
                ));

        for (var entry : itemsBySeller.entrySet()) {
            var seller = entry.getKey().orElse(null);
            var sellerItems = entry.getValue();

            var subTotal = sellerItems.stream()
                    .map(li -> li.listing().getPrice()
                            .multiply(BigDecimal.valueOf(li.cartItem().getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var subOrderType = seller == null ? Order.OrderType.B2C : Order.OrderType.C2C;

            var subOrder = Order.createSub(user, parentOrder, seller,
                    subTotal, subOrderType, request.shippingAddress());
            orderRepository.save(subOrder);

            for (var li : sellerItems) {
                var orderItem = OrderItem.create(subOrder, li.listing(), li.cartItem().getQuantity());
                subOrder.getItems().add(orderItem);
            }
            orderRepository.save(subOrder);
        }

        paymentService.createPaymentIntent(parentOrder, grandTotal);

        cartRepository.delete(cart);
        log.info("Order {} created for user {}", parentOrder.getId(), userId);

        return orderRepository.findWithDetailById(parentOrder.getId())
                .map(this::buildOrderResponse)
                .orElseThrow(OrderException::orderNotFound);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(int page, int size, Jwt jwt) {
        var userId = extractUserId(jwt);
        var pageable = PageableUtils.build(page, size, "createdAt,desc");

        return PageResponse.from(
                orderRepository.findByUserIdAndParentOrderIsNull(userId, pageable)
                        .map(this::buildOrderResponse)
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var order = orderRepository.findWithDetailById(orderId)
                .orElseThrow(OrderException::orderNotFound);

        var isOwner = order.getUser().getId().equals(userId);
        var isSellerOfSubOrder = !order.isParentOrder()
                && order.getSeller() != null
                && order.getSeller().getUser().getId().equals(userId);

        if (!isOwner && !isSellerOfSubOrder) {
            throw OrderException.accessDenied();
        }

        return buildOrderResponse(order);
    }

    @Transactional
    public void confirmOrder(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var order = orderRepository.findById(orderId)
                .orElseThrow(OrderException::orderNotFound);

        if (order.isParentOrder()) {
            throw OrderException.invalidStatusTransition();
        }

        if (order.getSeller() == null || !order.getSeller().getUser().getId().equals(userId)) {
            throw OrderException.accessDenied();
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw OrderException.invalidStatusTransition();
        }

        paymentService.ensureOrderPaymentCompleted(order.getParentOrder().getId());

        order.confirm();
    }

    @Transactional
    public void completeOrder(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var order = orderRepository.findWithDetailById(orderId)
                .orElseThrow(OrderException::orderNotFound);

        if (!order.getUser().getId().equals(userId)) {
            throw OrderException.accessDenied();
        }

        if (!order.isParentOrder()) {
            throw OrderException.invalidStatusTransition();
        }

        paymentService.ensureOrderPaymentCompleted(order.getId());

        var allReadyToComplete = order.getSubOrders().stream()
                .allMatch(sub -> sub.getOrderType() == Order.OrderType.B2C
                        || sub.getStatus() == Order.OrderStatus.CONFIRMED);

        if (!allReadyToComplete) {
            throw OrderException.invalidStatusTransition();
        }

        order.complete();
        order.getSubOrders().forEach(sub -> {
            sub.complete();

            if (sub.getOrderType() == Order.OrderType.C2C && sub.getSeller() != null) {
                releaseEscrow(sub);
            }
        });

        log.info("Order {} completed successfully", order.getId());
    }

    @Transactional
    public void cancelOrder(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var order = orderRepository.findWithDetailById(orderId)
                .orElseThrow(OrderException::orderNotFound);

        if (!order.getUser().getId().equals(userId)) {
            throw OrderException.accessDenied();
        }

        if (!order.isCancellable()) {
            throw OrderException.notCancellable();
        }

        order.cancel();

        if (order.isParentOrder()) {
            paymentService.cancelPaymentIfUnpaid(order.getId());
            order.getSubOrders().forEach(sub -> {
                sub.cancel();

                if (sub.getOrderType() == Order.OrderType.C2C) {
                    escrowRecordRepository.findByOrderId(sub.getId())
                            .ifPresent(escrow -> {
                                escrow.refund();
                                log.info("Escrow refunded for sub-order {}", sub.getId());
                            });
                }
            });
        }

        restoreStockForOrder(order);

        log.info("Order {} cancelled, stock restored", order.getId());
    }

    private void restoreStockForOrder(Order order) {
        var allItems = order.isParentOrder()
                ? order.getSubOrders().stream()
                .flatMap(sub -> sub.getItems().stream())
                .toList()
                : new ArrayList<>(order.getItems());

        if (allItems.isEmpty()) return;

        var sortedItems = allItems.stream()
                .sorted(Comparator.comparingLong(item -> item.getListing().getId()))
                .toList();

        for (var item : sortedItems) {
            var listing = bookListingRepository
                    .findByIdWithLock(item.getListing().getId())
                    .orElseThrow(ListingException::listingNotFound);

            listing.restoreStock(item.getQuantity());
            log.debug("Stock restored for listing {} — quantity +{}",
                    listing.getId(), item.getQuantity());
        }
    }

    private void releaseEscrow(Order subOrder) {
        var escrow = escrowRecordRepository.findByOrderId(subOrder.getId())
                .orElseThrow(() -> {
                    log.error("Escrow not found for sub-order {}", subOrder.getId());
                    return OrderException.orderNotFound();
                });

        var seller = subOrder.getSeller();
        var totalAmount = subOrder.getTotalAmount();
        var commissionRate = commissionProperties.rate();

        var commissionAmount = totalAmount
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        var netAmount = totalAmount.subtract(commissionAmount);

        var commission = PlatformCommission.create(subOrder, seller, commissionAmount, commissionRate);
        platformCommissionRepository.save(commission);

        var wallet = sellerWalletRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> {
                    log.error("Wallet not found for seller {}", seller.getId());
                    return OrderException.orderNotFound();
                });

        wallet.credit(netAmount);
        escrow.release();

        log.info("Escrow released for sub-order {} — total: {}, commission: {}, net: {}",
                subOrder.getId(), totalAmount, commissionAmount, netAmount);
    }

    private OrderResponse buildOrderResponse(Order order) {
        if (order.isParentOrder()) {
            var subOrderResponses = order.getSubOrders().stream()
                    .map(sub -> OrderResponse.fromSub(sub, buildItemResponses(sub.getItems())))
                    .toList();
            return OrderResponse.fromParent(order, List.of(), subOrderResponses);
        } else {
            return OrderResponse.fromSub(order, buildItemResponses(order.getItems()));
        }
    }

    private List<OrderItemResponse> buildItemResponses(Set<OrderItem> items) {
        if (items.isEmpty()) return List.of();

        var listingIds = items.stream()
                .map(item -> item.getListing().getId())
                .toList();

        var imagesByListingId = listingImageRepository
                .findByListingIdInOrderBySortOrder(listingIds)
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getListing().getId(),
                        Collectors.mapping(ListingImage::getImageUrl, Collectors.toList())
                ));

        return items.stream()
                .map(item -> OrderItemResponse.from(
                        item,
                        imagesByListingId.getOrDefault(item.getListing().getId(), List.of())
                ))
                .toList();
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private record LockedItem(CartItem cartItem, BookListing listing) {
    }
}