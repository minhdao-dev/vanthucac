package com.vanthucac.auction.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class AuctionItemTest {

    private AuctionItem item;

    @BeforeEach
    void setUp() {
        item = AuctionItem.create(null, null,
                BigDecimal.valueOf(500_000),
                BigDecimal.valueOf(50_000));
    }

    @Test
    void canBid_shouldReturnTrue_whenAmountIsExactlyMinRequired() {
        assertThat(item.canBid(BigDecimal.valueOf(550_000))).isTrue();
    }

    @Test
    void canBid_shouldReturnTrue_whenAmountExceedsMinRequired() {
        assertThat(item.canBid(BigDecimal.valueOf(700_000))).isTrue();
    }

    @Test
    void canBid_shouldReturnFalse_whenAmountBelowMinRequired() {
        assertThat(item.canBid(BigDecimal.valueOf(540_000))).isFalse();
    }

    @Test
    void canBid_shouldReturnFalse_whenAmountEqualsCurrentPrice() {
        assertThat(item.canBid(BigDecimal.valueOf(500_000))).isFalse();
    }
}