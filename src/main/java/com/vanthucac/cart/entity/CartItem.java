package com.vanthucac.cart.entity;

import com.vanthucac.listing.entity.BookListing;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private BookListing listing;

    @Column(nullable = false)
    private Integer quantity;

    public static CartItem create(Cart cart, BookListing listing, int quantity) {
        var item = new CartItem();
        item.cart = cart;
        item.listing = listing;
        item.quantity = quantity;
        return item;
    }

    public void addQuantity(int qty) {
        this.quantity += qty;
    }

    public void updateQuantity(int qty) {
        this.quantity = qty;
    }
}