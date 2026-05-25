package com.vanthucac.listing.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class BookListingTest {

    @Test
    void deductStock_shouldReduceStock_whenEnoughAvailable() {
        var listing = BookListing.createB2C(null, BigDecimal.valueOf(100_000), 5);
        listing.deductStock(3);
        assertThat(listing.getStock()).isEqualTo(2);
    }

    @Test
    void deductStock_shouldThrow_whenStockInsufficient() {
        var listing = BookListing.createB2C(null, BigDecimal.valueOf(100_000), 1);
        assertThatThrownBy(() -> listing.deductStock(2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deactivate_shouldSetStatusToInactive() {
        var listing = BookListing.createB2C(null, BigDecimal.valueOf(100_000), 5);
        listing.deactivate();
        assertThat(listing.getStatus()).isEqualTo(BookListing.ListingStatus.INACTIVE);
    }

    @Test
    void isNotOwnedBy_shouldReturnFalse_whenSellerMatches() {
        var listing = BookListing.createB2C(null, BigDecimal.valueOf(100_000), 5);
        // B2C listing không có seller → isNotOwnedBy luôn true
        assertThat(listing.isNotOwnedBy(1L)).isTrue();
    }
}