package com.vanthucac.order.service;

import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.entity.Cart;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.common.config.CommissionProperties;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.order.repository.OrderRepository;
import com.vanthucac.payment.repository.EscrowRecordRepository;
import com.vanthucac.payment.repository.PaymentRepository;
import com.vanthucac.payment.repository.PlatformCommissionRepository;
import com.vanthucac.seller.repository.SellerWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentRepository paymentRepository;
    @Mock
    EscrowRecordRepository escrowRecordRepository;
    @Mock
    PlatformCommissionRepository platformCommissionRepository;
    @Mock
    SellerWalletRepository sellerWalletRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    BookListingRepository bookListingRepository;
    @Mock
    ListingImageRepository listingImageRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    CommissionProperties commissionProperties;

    @InjectMocks
    OrderService orderService;

    private Jwt jwt;
    private User buyer;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn("10");

        buyer = User.create("buyer@example.com", "hash", "Buyer");
        setField(buyer, "id", 10L);
    }

    @Nested
    @DisplayName("checkout()")
    class CheckoutTests {

        @Test
        void checkout_throwsCartNotFound_whenNoCart() {
            given(userRepository.findById(10L)).willReturn(Optional.of(buyer));
            given(cartRepository.findByUserId(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderService.checkout(new CheckoutRequest("123 Street"), jwt))
                    .isInstanceOf(CartException.class);
        }

        @Test
        void checkout_throwsCartEmpty_whenCartIsEmpty() {
            given(userRepository.findById(10L)).willReturn(Optional.of(buyer));
            given(cartRepository.findByUserId(10L)).willReturn(Optional.of(Cart.create(buyer)));

            assertThatThrownBy(() ->
                    orderService.checkout(new CheckoutRequest("123 Street"), jwt))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("empty cart");
        }
    }

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrderTests {

        @Test
        void cancelOrder_throwsNotCancellable_whenCompleted() {
            given(orderRepository.findWithDetailById(100L))
                    .willReturn(Optional.of(buildParentOrder(Order.OrderStatus.COMPLETED)));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, jwt))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        void cancelOrder_throwsNotCancellable_whenShipping() {
            given(orderRepository.findWithDetailById(100L))
                    .willReturn(Optional.of(buildParentOrder(Order.OrderStatus.SHIPPING)));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, jwt))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        void cancelOrder_throwsAccessDenied_whenNotOwner() {
            var otherUser = User.create("other@example.com", "hash", "Other");
            setField(otherUser, "id", 99L);
            var order = Order.createParent(otherUser, BigDecimal.valueOf(100_000),
                    Order.OrderType.B2C, "123 Street");

            given(orderRepository.findWithDetailById(100L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(100L, jwt))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("access");
        }
    }

    @Nested
    @DisplayName("confirmOrder()")
    class ConfirmOrderTests {

        @Test
        void confirmOrder_throwsInvalidStatus_whenParentOrder() {
            given(orderRepository.findById(100L))
                    .willReturn(Optional.of(buildParentOrder(Order.OrderStatus.PENDING)));

            assertThatThrownBy(() -> orderService.confirmOrder(100L, jwt))
                    .isInstanceOf(OrderException.class)
                    .hasMessageContaining("Invalid order status transition");
        }
    }

    private Order buildParentOrder(Order.OrderStatus status) {
        var order = Order.createParent(buyer, BigDecimal.valueOf(100_000),
                Order.OrderType.B2C, "123 Street");
        setField(order, "status", status);
        return order;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}