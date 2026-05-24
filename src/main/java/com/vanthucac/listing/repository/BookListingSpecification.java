package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.BookListing;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class BookListingSpecification {

    private BookListingSpecification() {
    }

    public static Specification<BookListing> hasBookId(Long bookId) {
        return (root, query, cb) -> {
            if (bookId == null) return null;
            return cb.equal(root.get("bookCatalog").get("id"), bookId);
        };
    }

    public static Specification<BookListing> hasSellerId(Long sellerId) {
        return (root, query, cb) -> {
            if (sellerId == null) return null;
            return cb.equal(root.get("seller").get("id"), sellerId);
        };
    }

    public static Specification<BookListing> hasListingType(String listingType) {
        return (root, query, cb) -> {
            if (listingType == null || listingType.isBlank()) return null;
            return cb.equal(root.get("listingType"),
                    BookListing.ListingType.valueOf(listingType));
        };
    }

    public static Specification<BookListing> hasCondition(String condition) {
        return (root, query, cb) -> {
            if (condition == null || condition.isBlank()) return null;
            return cb.equal(root.get("condition"),
                    BookListing.BookCondition.valueOf(condition));
        };
    }

    public static Specification<BookListing> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice == null) return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
            if (maxPrice == null) return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            return cb.between(root.get("price"), minPrice, maxPrice);
        };
    }

    public static Specification<BookListing> isActive() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), BookListing.ListingStatus.ACTIVE);
    }
}