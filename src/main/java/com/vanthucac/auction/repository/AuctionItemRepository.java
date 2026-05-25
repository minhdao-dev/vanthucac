package com.vanthucac.auction.repository;

import com.vanthucac.auction.entity.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {

    Optional<AuctionItem> findById(Long id);

    List<AuctionItem> findBySessionId(Long sessionId);
}