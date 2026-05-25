package com.vanthucac.auction.repository;

import com.vanthucac.auction.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    Page<Bid> findByAuctionItemIdOrderByCreatedAtDesc(Long auctionItemId, Pageable pageable);

    boolean existsByAuctionItemIdAndUserId(Long auctionItemId, Long userId);
}