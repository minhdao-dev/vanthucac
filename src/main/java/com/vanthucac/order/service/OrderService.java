package com.vanthucac.order.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.outbox.OutboxEventService;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.dto.OrderItemResponse;
import com.vanthucac.order.dto.OrderResponse;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.entity.OrderItem;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.order.repository.OrderRepository;
import com.vanthucac.payment.service.EscrowReleaseService;
import com.vanthucac.payment.service.PaymentService;
import com.vanthucac.user.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final EscrowReleaseService escrowReleaseService;
    private final StockService stockService;
    private final CartRepository cartRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserRepository userRepository;
    private final OutboxEventService outboxEventService;

    public OrderService(
            OrderRepository orderRepository,
            PaymentService paymentService,
            EscrowReleaseService escrowReleaseService,
            StockService stockService,
            CartRepository cartRepository,
            ListingImageRepository listingImageRepository,
            UserRepository userRepository,
            OutboxEventService outboxEventService
    ) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.escrowReleaseService = escrowReleaseService;
        this.stockService = stockService;
        this.cartRepository = cartRepository;
        this.listingImageRepository = listingImageRepository;
        this.userRepository = userRepository;
        this.outboxEventService = outboxEventService;
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

        var lockedItems = stockService.lockAndDeduct(cart.getItems().stream().toList());

        var grandTotal = lockedItems.stream()
                .map(li -> li.listing().getPrice()
                        .multiply(BigDecimal.valueOf(li.cartItem().getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var hasC2C = lockedItems.stream()
                .anyMatch(li -> li.listing().getListingType() == BookListing.ListingType.C2C);
        var parentOrderType = hasC2C ? Order.OrderType.C2C : Order.OrderType.B2C;

        var parentOrder = Order.createParent(user, grandTotal, parentOrderType, request.shippingAddress());
        orderRepository.save(parentOrder);

        lockedItems.stream()
                .collect(Collectors.groupingBy(li -> {
                    var seller = li.listing().getSeller();
                    return seller != null ? seller.getId() : 0L;
                }))
                .forEach((sellerId, sellerItems) -> {
                    var seller = sellerItems.getFirst().listing().getSeller();
                    var subTotal = sellerItems.stream()
                            .map(li -> li.listing().getPrice()
                                    .multiply(BigDecimal.valueOf(li.cartItem().getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var subOrderType = seller == null ? Order.OrderType.B2C : Order.OrderType.C2C;

                    var subOrder = Order.createSub(user, parentOrder, seller,
                            subTotal, subOrderType, request.shippingAddress());

                    sellerItems.forEach(li ->
                            subOrder.getItems().add(OrderItem.create(subOrder, li.listing(), li.cartItem().getQuantity())));

                    orderRepository.save(subOrder);
                });

        paymentService.createPaymentIntent(parentOrder, grandTotal);

        outboxEventService.publish(
                "ORDER_CREATED",
                "ORDER",
                parentOrder.getId(),
                Map.of(
                        "orderId", parentOrder.getId(),
                        "userId", parentOrder.getUser().getId(),
                        "totalAmount", parentOrder.getTotalAmount(),
                        "orderType", parentOrder.getOrderType().name(),
                        "status", parentOrder.getStatus().name()
                )
        );

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

        paymentService.ensureOrderPaymentCompleted(order.getParentOrder().getId());

        order.confirm();

        outboxEventService.publish(
                "ORDER_CONFIRMED",
                "ORDER",
                order.getId(),
                Map.of(
                        "orderId", order.getId(),
                        "parentOrderId", order.getParentOrder().getId(),
                        "sellerId", order.getSeller().getId(),
                        "status", order.getStatus().name()
                )
        );
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
                escrowReleaseService.release(sub);
            }
        });

        outboxEventService.publish(
                "ORDER_COMPLETED",
                "ORDER",
                order.getId(),
                Map.of(
                        "orderId", order.getId(),
                        "userId", order.getUser().getId(),
                        "totalAmount", order.getTotalAmount(),
                        "status", order.getStatus().name()
                )
        );

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
                    escrowReleaseService.refund(sub);
                }
            });
        }

        stockService.restore(order);

        outboxEventService.publish(
                "ORDER_CANCELLED",
                "ORDER",
                order.getId(),
                Map.of(
                        "orderId", order.getId(),
                        "userId", order.getUser().getId(),
                        "status", order.getStatus().name()
                )
        );

        log.info("Order {} cancelled, stock restored", order.getId());
    }

    private OrderResponse buildOrderResponse(Order order) {
        if (order.isParentOrder()) {
            var subOrderResponses = order.getSubOrders().stream()
                    .map(sub -> OrderResponse.fromSub(sub, buildItemResponses(sub.getItems())))
                    .toList();
            return OrderResponse.fromParent(order, List.of(), subOrderResponses);
        }
        return OrderResponse.fromSub(order, buildItemResponses(order.getItems()));
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
}