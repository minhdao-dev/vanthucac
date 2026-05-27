package com.vanthucac.order.entity;

import com.vanthucac.auth.entity.User;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.seller.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    private Order parentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerProfile seller;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private Set<OrderItem> items = new HashSet<>();

    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL)
    private Set<Order> subOrders = new HashSet<>();

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPING,
        COMPLETED,
        CANCELLED;

        public boolean canTransitionTo(OrderStatus target) {
            return switch (this) {
                case PENDING -> target == CONFIRMED
                        || target == COMPLETED
                        || target == CANCELLED;
                case CONFIRMED -> target == SHIPPING
                        || target == COMPLETED
                        || target == CANCELLED;
                case SHIPPING -> target == COMPLETED;
                case COMPLETED, CANCELLED -> false;
            };
        }
    }

    public enum OrderType {
        B2C, C2C
    }

    public static Order createParent(User user, BigDecimal totalAmount,
                                     OrderType orderType, String shippingAddress) {
        var order = new Order();
        order.user = user;
        order.totalAmount = totalAmount;
        order.orderType = orderType;
        order.shippingAddress = shippingAddress;
        return order;
    }

    public static Order createSub(User user, Order parentOrder, SellerProfile seller,
                                  BigDecimal totalAmount, OrderType orderType,
                                  String shippingAddress) {
        var order = new Order();
        order.user = user;
        order.parentOrder = parentOrder;
        order.seller = seller;
        order.totalAmount = totalAmount;
        order.orderType = orderType;
        order.shippingAddress = shippingAddress;
        return order;
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void complete() {
        transitionTo(OrderStatus.COMPLETED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public boolean isCancellable() {
        return status.canTransitionTo(OrderStatus.CANCELLED);
    }

    public boolean isParentOrder() {
        return parentOrder == null;
    }

    private void transitionTo(OrderStatus targetStatus) {
        if (!status.canTransitionTo(targetStatus)) {
            throw OrderException.invalidStatusTransition();
        }
        this.status = targetStatus;
    }
}