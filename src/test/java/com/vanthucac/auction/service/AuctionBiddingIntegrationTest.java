package com.vanthucac.auction.service;

import com.vanthucac.auction.dto.PlaceBidRequest;
import com.vanthucac.auction.entity.AuctionItem;
import com.vanthucac.auction.entity.AuctionSession;
import com.vanthucac.auction.exception.AuctionException;
import com.vanthucac.auction.repository.AuctionItemRepository;
import com.vanthucac.auction.repository.AuctionSessionRepository;
import com.vanthucac.auction.repository.BidRepository;
import com.vanthucac.auth.entity.Role;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.RoleRepository;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.catalog.repository.BookCatalogRepository;
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

class AuctionBiddingIntegrationTest extends IntegrationTestBase {

    @Autowired
    AuctionService auctionService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    BookCatalogRepository bookCatalogRepository;
    @Autowired
    AuctionSessionRepository auctionSessionRepository;
    @Autowired
    AuctionItemRepository auctionItemRepository;
    @Autowired
    BidRepository bidRepository;

    private static final BigDecimal STARTING_PRICE = new BigDecimal("500000");
    private static final BigDecimal MIN_INCREMENT = new BigDecimal("50000");

    private Role buyerRole;

    @BeforeEach
    void loadFixedData() {
        buyerRole = roleRepository.findByName(Role.RoleName.BUYER).orElseThrow();
    }

    @Nested
    class HappyPath {

        @Test
        void updates_current_price_when_valid_bid_placed() {
            var bidder = saveUser("bidder-happy@test.com");
            var item = saveActiveItem("Rare Book Happy Path");

            var bidAmount = STARTING_PRICE.add(MIN_INCREMENT);
            var result = auctionService.placeBid(
                    item.getId(), new PlaceBidRequest(bidAmount), jwtFor(bidder)
            );

            assertThat(result.amount()).isEqualByComparingTo(bidAmount);
            assertThat(result.bidderId()).isEqualTo(bidder.getId());

            var updatedItem = auctionItemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getCurrentPrice()).isEqualByComparingTo(bidAmount);
            assertThat(updatedItem.getWinner().getId()).isEqualTo(bidder.getId());
        }

        @Test
        void records_bid_in_history() {
            var bidder = saveUser("bidder-history@test.com");
            var item = saveActiveItem("Rare Book History");

            var bidAmount = STARTING_PRICE.add(MIN_INCREMENT);
            auctionService.placeBid(item.getId(), new PlaceBidRequest(bidAmount), jwtFor(bidder));

            var bids = bidRepository.findByAuctionItemIdOrderByCreatedAtDesc(item.getId(), null);
            assertThat(bids.getContent()).hasSize(1);
            assertThat(bids.getContent().getFirst().getAmount()).isEqualByComparingTo(bidAmount);
        }
    }

    @Nested
    class BidValidation {

        @Test
        void throws_when_bid_amount_equals_starting_price() {
            var bidder = saveUser("bidder-low@test.com");
            var item = saveActiveItem("Rare Book Too Low");

            assertThatThrownBy(() ->
                    auctionService.placeBid(
                            item.getId(), new PlaceBidRequest(STARTING_PRICE), jwtFor(bidder)
                    )
            ).isInstanceOf(AuctionException.class);
        }

        @Test
        void throws_when_bid_amount_below_minimum_increment() {
            var bidder = saveUser("bidder-below@test.com");
            var item = saveActiveItem("Rare Book Below Min");

            assertThatThrownBy(() ->
                    auctionService.placeBid(
                            item.getId(),
                            new PlaceBidRequest(STARTING_PRICE.add(new BigDecimal("10000"))),
                            jwtFor(bidder)
                    )
            ).isInstanceOf(AuctionException.class);
        }
    }

    @Nested
    class ConcurrentBidding {

        @Test
        void only_one_bid_wins_when_two_users_place_same_amount_concurrently() throws InterruptedException {
            var userA = saveUser("concurrent-bidder-a@test.com");
            var userB = saveUser("concurrent-bidder-b@test.com");
            var item = saveActiveItem("Rare Book Concurrent Bid");

            var sameAmount = STARTING_PRICE.add(MIN_INCREMENT);
            var request = new PlaceBidRequest(sameAmount);
            var latch = new CountDownLatch(1);
            var successCount = new AtomicInteger(0);
            var failures = new CopyOnWriteArrayList<Exception>();

            try (var executor = Executors.newFixedThreadPool(2)) {
                executor.submit(() -> {
                    try {
                        latch.await();
                        auctionService.placeBid(item.getId(), request, jwtFor(userA));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failures.add(e);
                    }
                });

                executor.submit(() -> {
                    try {
                        latch.await();
                        auctionService.placeBid(item.getId(), request, jwtFor(userB));
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

            var finalItem = auctionItemRepository.findById(item.getId()).orElseThrow();
            assertThat(finalItem.getCurrentPrice()).isEqualByComparingTo(sameAmount);
            assertThat(finalItem.getWinner()).isNotNull();

            var totalBids = bidRepository.findByAuctionItemIdOrderByCreatedAtDesc(item.getId(), null);
            assertThat(totalBids.getContent()).hasSize(1);
        }
    }

    private User saveUser(String email) {
        var user = User.create(email, "hashed-password", "Test Bidder");
        user.addRole(buyerRole);
        return userRepository.save(user);
    }

    private AuctionItem saveActiveItem(String bookTitle) {
        var admin = saveUser(UUID.randomUUID() + "@admin-test.com");
        var catalog = BookCatalog.create(
                null, bookTitle, "Classic Author",
                "Rare Publisher", null, null, "Collectible"
        );
        bookCatalogRepository.save(catalog);

        var session = AuctionSession.create(
                "Test Session",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600),
                admin
        );
        auctionSessionRepository.save(session);
        session.activate();
        auctionSessionRepository.save(session);

        var item = AuctionItem.create(session, catalog, STARTING_PRICE, MIN_INCREMENT);
        auctionItemRepository.save(item);
        item.activate();
        return auctionItemRepository.save(item);
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