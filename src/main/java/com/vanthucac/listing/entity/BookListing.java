package com.vanthucac.listing.entity;

import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.seller.entity.SellerProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "book_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_catalog_id", nullable = false)
    private BookCatalog bookCatalog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private SellerProfile seller;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", nullable = false, length = 20)
    private BookCondition condition;

    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.PENDING_REVIEW;

    @Version
    private Integer version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum BookCondition {
        NEW, LIKE_NEW, GOOD, FAIR
    }

    public enum ListingType {
        B2C, C2C
    }

    public enum ListingStatus {
        PENDING_REVIEW, ACTIVE, INACTIVE
    }

    public static BookListing createC2C(
            BookCatalog bookCatalog,
            SellerProfile seller,
            BigDecimal price,
            BookCondition condition,
            Integer stock
    ) {
        var listing = new BookListing();
        listing.bookCatalog = bookCatalog;
        listing.seller = seller;
        listing.price = price;
        listing.condition = condition;
        listing.stock = stock;
        listing.listingType = ListingType.C2C;
        listing.status = ListingStatus.PENDING_REVIEW;
        return listing;
    }

    public static BookListing createB2C(
            BookCatalog bookCatalog,
            BigDecimal price,
            Integer stock
    ) {
        var listing = new BookListing();
        listing.bookCatalog = bookCatalog;
        listing.seller = null;
        listing.price = price;
        listing.condition = BookCondition.NEW;
        listing.stock = stock;
        listing.listingType = ListingType.B2C;
        listing.status = ListingStatus.ACTIVE;
        return listing;
    }

    public void update(BigDecimal price, BookCondition condition, Integer stock, ListingStatus status) {
        this.price = price;
        this.condition = condition;
        this.stock = stock;
        this.status = status;
    }

    public void deactivate() {
        this.status = ListingStatus.INACTIVE;
    }

    public boolean isNotOwnedBy(Long sellerId) {
        return seller == null || !seller.getId().equals(sellerId);
    }

    public void deductStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.stock -= quantity;
    }
}