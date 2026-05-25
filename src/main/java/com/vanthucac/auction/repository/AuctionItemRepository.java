package com.vanthucac.auction.repository;

import com.vanthucac.auction.entity.AuctionItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM AuctionItem i WHERE i.id = :id")
    Optional<AuctionItem> findByIdWithLock(Long id);

    List<AuctionItem> findBySessionId(Long sessionId);
}