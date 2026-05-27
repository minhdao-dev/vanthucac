package com.vanthucac.auction.service;

import com.vanthucac.auction.dto.CreateAuctionSessionRequest;
import com.vanthucac.auction.dto.PlaceBidRequest;
import com.vanthucac.auction.entity.AuctionItem;
import com.vanthucac.auction.entity.AuctionSession;
import com.vanthucac.auction.exception.AuctionException;
import com.vanthucac.auction.repository.AuctionItemRepository;
import com.vanthucac.auction.repository.AuctionSessionRepository;
import com.vanthucac.auction.repository.BidRepository;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.catalog.repository.BookCatalogRepository;
import com.vanthucac.notification.service.EmailNotificationService;
import com.vanthucac.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class AuctionServiceTest {

    @Mock
    AuctionSessionRepository auctionSessionRepository;
    @Mock
    AuctionItemRepository auctionItemRepository;
    @Mock
    BidRepository bidRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    BookCatalogRepository bookCatalogRepository;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    EmailNotificationService emailNotificationService;
    @Mock
    NotificationService notificationService;

    @InjectMocks
    AuctionService auctionService;

    private Jwt jwt;
    private User bidder;
    private AuctionItem activeItem;

    @BeforeEach
    void setUp() {
        jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn("42");

        bidder = User.create("bidder@example.com", "hash", "Bidder");
        setField(bidder, "id", 42L);

        var session = AuctionSession.create(
                "Test Session",
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                User.create("admin@example.com", "hash", "Admin")
        );
        session.activate();

        var book = BookCatalog.create(null, "Dune", "Frank Herbert", null, null, null, null);
        activeItem = AuctionItem.create(session, book,
                BigDecimal.valueOf(500_000), BigDecimal.valueOf(50_000));
        setField(activeItem, "id", 7L);
    }

    @Nested
    @DisplayName("placeBid()")
    class PlaceBidTests {

        @Test
        void placeBid_success() {
            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));
            given(auctionItemRepository.findByIdWithLock(7L)).willReturn(Optional.of(activeItem));
            given(bidRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = auctionService.placeBid(
                    7L, new PlaceBidRequest(BigDecimal.valueOf(600_000)), jwt);

            assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
            assertThat(activeItem.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
        }

        @Test
        void placeBid_throwsBidTooLow() {
            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));
            given(auctionItemRepository.findByIdWithLock(7L)).willReturn(Optional.of(activeItem));

            assertThatThrownBy(() -> auctionService.placeBid(
                    7L, new PlaceBidRequest(BigDecimal.valueOf(540_000)), jwt))
                    .isInstanceOf(AuctionException.class)
                    .hasMessageContaining("550000");
        }

        @Test
        void placeBid_throwsSessionNotActive_whenScheduled() {
            var scheduledSession = AuctionSession.create(
                    "Future", Instant.now().plusSeconds(3600),
                    Instant.now().plusSeconds(7200), bidder);
            var book = BookCatalog.create(null, "Test", null, null, null, null, null);
            var waitingItem = AuctionItem.create(scheduledSession, book,
                    BigDecimal.valueOf(500_000), BigDecimal.valueOf(50_000));
            setField(waitingItem, "id", 99L);

            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));
            given(auctionItemRepository.findByIdWithLock(99L)).willReturn(Optional.of(waitingItem));

            assertThatThrownBy(() -> auctionService.placeBid(
                    99L, new PlaceBidRequest(BigDecimal.valueOf(600_000)), jwt))
                    .isInstanceOf(AuctionException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        void placeBid_throwsBidOnOwnItem_whenBidderIsWinner() {
            activeItem.placeBid(BigDecimal.valueOf(600_000), bidder);

            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));
            given(auctionItemRepository.findByIdWithLock(7L)).willReturn(Optional.of(activeItem));

            assertThatThrownBy(() -> auctionService.placeBid(
                    7L, new PlaceBidRequest(BigDecimal.valueOf(700_000)), jwt))
                    .isInstanceOf(AuctionException.class)
                    .hasMessageContaining("cannot bid on your own");
        }

        @Test
        void placeBid_notifiesPreviousWinner_whenOutbid() {
            var previousWinner = User.create("prev@example.com", "hash", "Prev");
            setField(previousWinner, "id", 99L);
            activeItem.placeBid(BigDecimal.valueOf(600_000), previousWinner);

            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));
            given(auctionItemRepository.findByIdWithLock(7L)).willReturn(Optional.of(activeItem));
            given(bidRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            auctionService.placeBid(7L, new PlaceBidRequest(BigDecimal.valueOf(700_000)), jwt);

            then(notificationService).should().notifyAuctionOutbid(eq(previousWinner), anyString());
        }
    }

    @Nested
    @DisplayName("createSession()")
    class CreateSessionTests {

        @Test
        void createSession_throwsInvalidTime_whenEndBeforeStart() {
            given(userRepository.findById(42L)).willReturn(Optional.of(bidder));

            var now = Instant.now();
            assertThatThrownBy(() -> auctionService.createSession(
                    new CreateAuctionSessionRequest("Test",
                            now.plusSeconds(3600),
                            now.plusSeconds(1800)),
                    jwt))
                    .isInstanceOf(AuctionException.class)
                    .hasMessageContaining("End time must be after start time");
        }
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