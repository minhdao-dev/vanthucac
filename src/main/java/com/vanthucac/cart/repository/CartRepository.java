package com.vanthucac.cart.repository;

import com.vanthucac.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {
            "items",
            "items.listing",
            "items.listing.bookCatalog",
            "items.listing.seller"
    })
    Optional<Cart> findByUserId(Long userId);
}