package com.vanthucac.listing.dto;

import com.vanthucac.catalog.dto.BookCatalogResponse;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.seller.dto.SellerProfileResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingResponse(
        Integer id,
        BookCatalogResponse book,
        SellerProfileResponse seller,
        BigDecimal price,
        String condition,
        Integer stock,
        String listingType,
        String status,
        List<String> imageUrls,
        Instant createdAt
) {
    public static ListingResponse from(BookListing listing, List<String> imageUrls) {
        return new ListingResponse(
                listing.getId(),
                BookCatalogResponse.from(listing.getBookCatalog()),
                listing.getSeller() != null
                        ? SellerProfileResponse.from(listing.getSeller())
                        : null,
                listing.getPrice(),
                listing.getCondition().name(),
                listing.getStock(),
                listing.getListingType().name(),
                listing.getStatus().name(),
                imageUrls,
                listing.getCreatedAt()
        );
    }
}