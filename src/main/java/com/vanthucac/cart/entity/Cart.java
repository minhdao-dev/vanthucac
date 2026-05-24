package com.vanthucac.cart.entity;

import com.vanthucac.auth.entity.User;
import com.vanthucac.listing.entity.BookListing;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // cascade=ALL + orphanRemoval: remove khỏi list → Hibernate tự DELETE row
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Cart create(User user) {
        var cart = new Cart();
        cart.user = user;
        cart.updatedAt = Instant.now();
        return cart;
    }

    public void addOrMergeItem(BookListing listing, int quantity) {
        findItemByListingId(listing.getId())
                .ifPresentOrElse(
                        existing -> existing.addQuantity(quantity),
                        () -> items.add(CartItem.create(this, listing, quantity))
                );
        touch();
    }

    public Optional<CartItem> findItemById(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    private Optional<CartItem> findItemByListingId(Long listingId) {
        return items.stream()
                .filter(item -> item.getListing().getId().equals(listingId))
                .findFirst();
    }
}