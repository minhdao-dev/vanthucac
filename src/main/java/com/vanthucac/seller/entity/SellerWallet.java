package com.vanthucac.seller.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seller_wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private SellerProfile seller;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static SellerWallet create(SellerProfile seller) {
        var wallet = new SellerWallet();
        wallet.seller = seller;
        wallet.updatedAt = Instant.now();
        return wallet;
    }
}