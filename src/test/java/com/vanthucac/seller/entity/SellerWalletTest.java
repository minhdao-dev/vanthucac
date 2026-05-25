package com.vanthucac.seller.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class SellerWalletTest {

    private SellerWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = SellerWallet.create(null);
    }

    @Test
    void credit_shouldIncreaseBalance() {
        wallet.credit(BigDecimal.valueOf(100_000));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    }

    @Test
    void credit_shouldAccumulate_whenCalledMultipleTimes() {
        wallet.credit(BigDecimal.valueOf(100_000));
        wallet.credit(BigDecimal.valueOf(50_000));
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
    }

    @Test
    void credit_shouldThrow_whenAmountIsZero() {
        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void credit_shouldThrow_whenAmountIsNegative() {
        assertThatThrownBy(() -> wallet.credit(BigDecimal.valueOf(-1000)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}