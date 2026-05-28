package com.vanthucac.order.service;

import com.vanthucac.auth.entity.Role;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.RoleRepository;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.entity.Cart;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.catalog.repository.BookCatalogRepository;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.exception.OrderException;
import com.vanthucac.order.repository.OrderRepository;
import com.vanthucac.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    OrderService orderService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    BookCatalogRepository bookCatalogRepository;
    @Autowired
    BookListingRepository bookListingRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
    OrderRepository orderRepository;

    private Role buyerRole;

    @BeforeEach
    void loadFixedData() {
        buyerRole = roleRepository.findByName(Role.RoleName.BUYER).orElseThrow();
    }

    @Nested
    class HappyPath {

        @Test
        void creates_order_and_deducts_stock_on_successful_checkout() {
            var user = saveUser("buyer-happy@test.com");
            var listing = saveActiveListing("Dune", 3);
            saveCart(user, listing, 2);

            var result = orderService.checkout(
                    new CheckoutRequest("123 Test Street"), jwtFor(user)
            );

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("PENDING");
            assertThat(result.totalAmount()).isEqualByComparingTo(
                    listing.getPrice().multiply(BigDecimal.valueOf(2))
            );
            assertThat(result.subOrders()).hasSize(1);

            var stockAfter = bookListingRepository.findById(listing.getId()).orElseThrow().getStock();
            assertThat(stockAfter).isEqualTo(1);
        }

        @Test
        void clears_cart_after_successful_checkout() {
            var user = saveUser("buyer-cart@test.com");
            var listing = saveActiveListing("Foundation", 5);
            saveCart(user, listing, 1);

            orderService.checkout(new CheckoutRequest("456 Test Avenue"), jwtFor(user));

            var cartAfter = cartRepository.findByUserId(user.getId());
            assertThat(cartAfter).isEmpty();
        }
    }

    @Nested
    class StockValidation {

        @Test
        void throws_when_listing_has_no_stock() {
            var user = saveUser("buyer-nostock@test.com");
            var listing = saveActiveListing("Empty Book", 1);
            listing.deductStock(1);
            bookListingRepository.save(listing);
            saveCart(user, listing, 1);

            assertThatThrownBy(() ->
                    orderService.checkout(new CheckoutRequest("789 Test Blvd"), jwtFor(user))
            ).isInstanceOf(OrderException.class);
        }

        @Test
        void throws_when_requested_quantity_exceeds_stock() {
            var user = saveUser("buyer-exceed@test.com");
            var listing = saveActiveListing("Limited Book", 1);
            saveCart(user, listing, 2);

            assertThatThrownBy(() ->
                    orderService.checkout(new CheckoutRequest("321 Test Road"), jwtFor(user))
            ).isInstanceOf(OrderException.class);
        }
    }

    @Nested
    class ConcurrentCheckout {

        @Test
        void only_one_checkout_succeeds_when_two_users_race_for_last_item() throws InterruptedException {
            var userA = saveUser("race-a@test.com");
            var userB = saveUser("race-b@test.com");
            var sharedListing = saveActiveListing("Rare Book", 1);
            saveCart(userA, sharedListing, 1);
            saveCart(userB, sharedListing, 1);

            var request = new CheckoutRequest("Race Condition Street");
            var latch = new CountDownLatch(1);
            var successCount = new AtomicInteger(0);
            var failures = new CopyOnWriteArrayList<Exception>();

            try (var executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    try {
                        latch.await();
                        orderService.checkout(request, jwtFor(userA));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failures.add(e);
                    }
                });

                executor.submit(() -> {
                    try {
                        latch.await();
                        orderService.checkout(request, jwtFor(userB));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failures.add(e);
                    }
                });

                latch.countDown();
                executor.shutdown();
                assertThat(executor.awaitTermination(15, TimeUnit.SECONDS))
                        .as("Executor did not finish within timeout")
                        .isTrue();
            }

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failures).hasSize(1);

            var finalStock = bookListingRepository.findById(sharedListing.getId())
                    .orElseThrow()
                    .getStock();
            assertThat(finalStock).isEqualTo(0);

            var orderCount = orderRepository.count();
            assertThat(orderCount).isEqualTo(2);
        }
    }

    private User saveUser(String email) {
        var user = User.create(email, "hashed-password", "Test User");
        user.addRole(buyerRole);
        return userRepository.save(user);
    }

    private BookListing saveActiveListing(String title, int stock) {
        var catalog = BookCatalog.create(
                UUID.randomUUID().toString().substring(0, 13),
                title, "Test Author", "Test Publisher",
                null, null, "Fiction"
        );
        bookCatalogRepository.save(catalog);

        var listing = BookListing.createB2C(catalog, new BigDecimal("150000"), stock);
        listing.approve();
        return bookListingRepository.save(listing);
    }

    private void saveCart(User user, BookListing listing, int quantity) {
        var cart = Cart.create(user);
        cart.addOrMergeItem(listing, quantity);
        cartRepository.save(cart);
    }

    private Jwt jwtFor(User user) {
        return Jwt.withTokenValue("test-token-" + user.getId())
                .header("alg", "HS256")
                .subject(user.getId().toString())
                .claim("roles", List.of("BUYER"))
                .claim("sessionId", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}