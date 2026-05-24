package com.vanthucac.order.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.catalog.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.dto.OrderItemResponse;
import com.vanthucac.order.dto.OrderResponse;
import com.vanthucac.order.entity.EscrowRecord;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.entity.OrderItem;
import com.vanthucac.order.entity.Payment;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.order.repository.EscrowRecordRepository;
import com.vanthucac.order.repository.OrderRepository;
import com.vanthucac.order.repository.PaymentRepository;
import com.vanthucac.seller.entity.SellerProfile;
import com.vanthucac.user.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EscrowRecordRepository escrowRecordRepository;
    private final CartRepository cartRepository;
    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserRepository userRepository;

    public OrderService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            EscrowRecordRepository escrowRecordRepository,
            CartRepository cartRepository,
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.escrowRecordRepository = escrowRecordRepository;
        this.cartRepository = cartRepository;
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.userRepository = userRepository;
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

        var cartItems = cart.getItems();

        var sortedItems = cartItems.stream()
                .sorted(Comparator.comparingLong(item -> item.getListing().getId()))
                .toList();

        var lockedListings = new ArrayList<LockedItem>();
        for (var cartItem : sortedItems) {
            var listing = bookListingRepository
                    .findByIdWithLock(cartItem.getListing().getId())
                    .orElseThrow(() -> OrderException.stockInsufficient(
                            cartItem.getListing().getBookCatalog().getTitle()));

            if (listing.getStatus() != BookListing.ListingStatus.ACTIVE) {
                throw OrderException.stockInsufficient(
                        listing.getBookCatalog().getTitle());
            }

            if (listing.getStock() < cartItem.getQuantity()) {
                throw OrderException.stockInsufficient(
                        listing.getBookCatalog().getTitle());
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

        Map<SellerProfile, List<LockedItem>> itemsBySeller = lockedListings.stream()
                .collect(Collectors.groupingBy(
                        li -> li.listing().getSeller()
                ));

        for (var entry : itemsBySeller.entrySet()) {
            var seller = entry.getKey();
            var sellerItems = entry.getValue();

            var subTotal = sellerItems.stream()
                    .map(li -> li.listing().getPrice()
                            .multiply(BigDecimal.valueOf(li.cartItem().getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var subOrderType = seller == null
                    ? Order.OrderType.B2C
                    : Order.OrderType.C2C;

            var subOrder = Order.createSub(user, parentOrder, seller,
                    subTotal, subOrderType, request.shippingAddress());
            orderRepository.save(subOrder);

            for (var li : sellerItems) {
                var orderItem = OrderItem.create(subOrder, li.listing(),
                        li.cartItem().getQuantity());
                subOrder.getItems().add(orderItem);
            }
            orderRepository.save(subOrder);

            if (subOrderType == Order.OrderType.C2C) {
                var escrow = EscrowRecord.create(subOrder, subTotal);
                escrowRecordRepository.save(escrow);
                log.info("Escrow created for sub-order {} — amount {}",
                        subOrder.getId(), subTotal);
            }
        }

        var payment = Payment.createMock(parentOrder, grandTotal);
        paymentRepository.save(payment);

        cartRepository.delete(cart);

        log.info("Order {} created successfully for user {}", parentOrder.getId(), userId);

        var savedOrder = orderRepository.findWithDetailById(parentOrder.getId())
                .orElseThrow(OrderException::orderNotFound);

        return buildOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(int page, int size, Jwt jwt) {
        var userId = extractUserId(jwt);
        var pageable = PageableUtils.build(page, size, "createdAt,desc");

        return PageResponse.from(
                orderRepository.findByUserIdAndParentOrderIsNull(userId, pageable)
                        .map(order -> {
                            var full = orderRepository.findWithDetailById(order.getId())
                                    .orElse(order);
                            return buildOrderResponse(full);
                        })
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

        if (order.getSeller() == null
                || !order.getSeller().getUser().getId().equals(userId)) {
            throw OrderException.accessDenied();
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw OrderException.invalidStatusTransition();
        }

        order.confirm();
    }

    @Transactional
    public void completeOrder(Long orderId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var order = orderRepository.findById(orderId)
                .orElseThrow(OrderException::orderNotFound);

        if (!order.getUser().getId().equals(userId)) {
            throw OrderException.accessDenied();
        }

        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw OrderException.invalidStatusTransition();
        }

        order.complete();
        log.info("Order {} completed — escrow release will be handled in Phase 3",
                order.getId());
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
            order.getSubOrders().forEach(Order::cancel);
        }
    }

    private OrderResponse buildOrderResponse(Order order) {
        if (order.isParentOrder()) {
            var subOrderResponses = order.getSubOrders().stream()
                    .map(sub -> {
                        var items = buildItemResponses(sub.getItems());
                        return OrderResponse.fromSub(sub, items);
                    })
                    .collect(Collectors.toSet());

            return OrderResponse.fromParent(order, Set.of(), subOrderResponses);
        } else {
            var items = buildItemResponses(order.getItems());
            return OrderResponse.fromSub(order, items);
        }
    }

    private Set<OrderItemResponse> buildItemResponses(Set<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    var images = listingImageRepository
                            .findByListingIdOrderBySortOrder(item.getListing().getId())
                            .stream()
                            .map(ListingImage::getImageUrl)
                            .toList();
                    return OrderItemResponse.from(item, images);
                })
                .collect(Collectors.toSet());
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private record LockedItem(CartItem cartItem, BookListing listing) {
    }
}