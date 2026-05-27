package com.vanthucac.auction.service;

import com.vanthucac.auction.dto.*;
import com.vanthucac.auction.entity.AuctionItem;
import com.vanthucac.auction.entity.AuctionSession;
import com.vanthucac.auction.entity.Bid;
import com.vanthucac.auction.exception.AuctionErrorCode;
import com.vanthucac.auction.exception.AuctionException;
import com.vanthucac.auction.repository.AuctionItemRepository;
import com.vanthucac.auction.repository.AuctionSessionRepository;
import com.vanthucac.auction.repository.BidRepository;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.catalog.repository.BookCatalogRepository;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.notification.outbox.NotificationOutboxEventPublisher;
import com.vanthucac.user.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionSessionRepository auctionSessionRepository;
    private final AuctionItemRepository auctionItemRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final BookCatalogRepository bookCatalogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationOutboxEventPublisher notificationOutboxEventPublisher;

    public AuctionService(
            AuctionSessionRepository auctionSessionRepository,
            AuctionItemRepository auctionItemRepository,
            BidRepository bidRepository,
            UserRepository userRepository,
            BookCatalogRepository bookCatalogRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationOutboxEventPublisher notificationOutboxEventPublisher
    ) {
        this.auctionSessionRepository = auctionSessionRepository;
        this.auctionItemRepository = auctionItemRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.bookCatalogRepository = bookCatalogRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationOutboxEventPublisher = notificationOutboxEventPublisher;
    }

    @Transactional
    public AuctionSessionResponse createSession(CreateAuctionSessionRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);
        var admin = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        if (request.endTime().isBefore(request.startTime())) {
            throw AuctionException.invalidTime();
        }

        var session = AuctionSession.create(
                request.title(),
                request.startTime(),
                request.endTime(),
                admin
        );
        auctionSessionRepository.save(session);

        return AuctionSessionResponse.from(session);
    }

    @Transactional
    public AuctionItemResponse addItem(Long sessionId, CreateAuctionItemRequest request) {
        var session = auctionSessionRepository.findById(sessionId)
                .orElseThrow(AuctionException::sessionNotFound);

        if (!session.isScheduled()) {
            throw AuctionException.sessionNotScheduled();
        }

        var bookCatalog = bookCatalogRepository.findById(request.bookCatalogId())
                .orElseThrow(AuctionException::itemNotFound);

        var item = AuctionItem.create(
                session,
                bookCatalog,
                request.startingPrice(),
                request.minBidIncrement()
        );
        auctionItemRepository.save(item);

        return AuctionItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuctionSessionResponse> getSessions(String status, int page, int size) {
        var pageable = PageableUtils.build(page, size, "startTime,desc");

        if (status != null && !status.isBlank()) {
            var sessionStatus = parseSessionStatus(status);
            return PageResponse.from(
                    auctionSessionRepository.findByStatus(sessionStatus, pageable)
                            .map(AuctionSessionResponse::from)
            );
        }

        return PageResponse.from(
                auctionSessionRepository.findAll(pageable)
                        .map(AuctionSessionResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public AuctionSessionResponse getSessionById(Long sessionId) {
        return auctionSessionRepository.findWithItemsById(sessionId)
                .map(AuctionSessionResponse::from)
                .orElseThrow(AuctionException::sessionNotFound);
    }

    @Transactional
    public BidResponse placeBid(Long itemId, PlaceBidRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);
        var user = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        var item = auctionItemRepository.findByIdWithLock(itemId)
                .orElseThrow(AuctionException::itemNotFound);

        if (!item.getSession().isActive()) {
            throw AuctionException.sessionNotActive();
        }

        if (item.getWinner() != null && item.getWinner().getId().equals(userId)) {
            throw AuctionException.bidOnOwnItem();
        }

        if (!item.canBid(request.amount())) {
            var minRequired = item.getCurrentPrice().add(item.getMinBidIncrement());
            throw AuctionException.bidTooLow(minRequired.toPlainString());
        }

        final User previousWinner = item.getWinner();
        final String bookTitle = item.getBookCatalog().getTitle();

        var bid = Bid.create(item, user, request.amount());
        bidRepository.save(bid);

        item.placeBid(request.amount(), user);

        log.info("Bid placed on item {} by user {} — amount {}", itemId, userId, request.amount());

        if (previousWinner != null && !previousWinner.getId().equals(userId)) {
            notificationOutboxEventPublisher.publishAuctionOutbidNotification(
                    itemId,
                    previousWinner.getId(),
                    bookTitle
            );
        }

        final var broadcastMessage = new BidBroadcastMessage(
                itemId,
                request.amount(),
                userId,
                user.getFullName(),
                Instant.now()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/auction/" + itemId, broadcastMessage);
                log.debug("Broadcast sent for item {} after transaction commit", itemId);
            }
        });

        return BidResponse.from(bid);
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponse> getBidHistory(Long itemId, int page, int size) {
        if (!auctionItemRepository.existsById(itemId)) {
            throw AuctionException.itemNotFound();
        }

        var pageable = PageableUtils.build(page, size, "createdAt,desc");

        return PageResponse.from(
                bidRepository.findByAuctionItemIdOrderByCreatedAtDesc(itemId, pageable)
                        .map(BidResponse::from)
        );
    }

    @Transactional
    public void processScheduledSessions() {
        var now = Instant.now();

        var toActivate = auctionSessionRepository
                .findByStatusAndStartTimeBefore(AuctionSession.SessionStatus.SCHEDULED, now);

        for (var session : toActivate) {
            session.activate();
            session.getItems().forEach(AuctionItem::activate);
            log.info("Auction session {} activated", session.getId());
        }

        var toClose = auctionSessionRepository
                .findByStatusAndEndTimeBefore(AuctionSession.SessionStatus.ACTIVE, now);

        for (var session : toClose) {
            session.close();
            closeSessionItems(session);
            log.info("Auction session {} closed", session.getId());
        }
    }

    private void closeSessionItems(AuctionSession session) {
        for (var item : session.getItems()) {
            if (item.getWinner() != null) {
                item.sold();
                publishAuctionWinnerEvents(item);
                log.info("Item {} sold to user {}", item.getId(), item.getWinner().getId());
            } else {
                item.unsold();
                log.info("Item {} unsold", item.getId());
            }
        }
    }

    private void publishAuctionWinnerEvents(AuctionItem item) {
        var winner = item.getWinner();

        notificationOutboxEventPublisher.publishAuctionWinnerEmail(
                item.getId(),
                winner.getId(),
                winner.getEmail(),
                winner.getFullName(),
                item.getBookCatalog().getTitle(),
                item.getCurrentPrice()
        );

        notificationOutboxEventPublisher.publishAuctionWonNotification(
                item.getId(),
                winner.getId(),
                item.getBookCatalog().getTitle()
        );
    }

    private AuctionSession.SessionStatus parseSessionStatus(String status) {
        try {
            return AuctionSession.SessionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuctionException(
                    "Invalid session status: " + status
                            + ". Valid values: SCHEDULED, ACTIVE, CLOSED",
                    AuctionErrorCode.SESSION_NOT_FOUND,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}