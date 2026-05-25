package com.vanthucac.auction.entity;

import com.vanthucac.auth.entity.User;
import com.vanthucac.catalog.entity.BookCatalog;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "auction_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuctionSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_catalog_id", nullable = false)
    private BookCatalog bookCatalog;

    @Column(name = "starting_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "min_bid_increment", nullable = false, precision = 15, scale = 2)
    private BigDecimal minBidIncrement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.WAITING;

    @Version
    private Integer version;

    public enum ItemStatus {
        WAITING, ACTIVE, SOLD, UNSOLD
    }

    public static AuctionItem create(AuctionSession session, BookCatalog bookCatalog,
                                     BigDecimal startingPrice, BigDecimal minBidIncrement) {
        var item = new AuctionItem();
        item.session = session;
        item.bookCatalog = bookCatalog;
        item.startingPrice = startingPrice;
        item.currentPrice = startingPrice;
        item.minBidIncrement = minBidIncrement;
        return item;
    }

    public void placeBid(BigDecimal amount, User bidder) {
        this.currentPrice = amount;
        this.winner = bidder;
    }

    public void activate() {
        this.status = ItemStatus.ACTIVE;
    }

    public void sold() {
        this.status = ItemStatus.SOLD;
    }

    public void unsold() {
        this.status = ItemStatus.UNSOLD;
    }

    public boolean canBid(BigDecimal amount) {
        return amount.compareTo(currentPrice.add(minBidIncrement)) >= 0;
    }
}