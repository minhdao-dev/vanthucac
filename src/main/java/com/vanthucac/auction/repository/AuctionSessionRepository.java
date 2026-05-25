package com.vanthucac.auction.repository;

import com.vanthucac.auction.entity.AuctionSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionSessionRepository extends JpaRepository<AuctionSession, Long> {

    Page<AuctionSession> findByStatus(AuctionSession.SessionStatus status, Pageable pageable);

    Page<AuctionSession> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.bookCatalog", "items.winner"})
    Optional<AuctionSession> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items"})
    List<AuctionSession> findByStatusAndStartTimeBefore(
            AuctionSession.SessionStatus status, Instant now);

    @EntityGraph(attributePaths = {"items", "items.bookCatalog", "items.winner"})
    List<AuctionSession> findByStatusAndEndTimeBefore(
            AuctionSession.SessionStatus status, Instant now);
}