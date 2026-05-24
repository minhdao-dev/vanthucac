package com.vanthucac.order.repository;

import com.vanthucac.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdAndParentOrderIsNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "items", "items.listing", "items.listing.bookCatalog",
            "seller", "subOrders", "subOrders.items",
            "subOrders.items.listing", "subOrders.items.listing.bookCatalog",
            "subOrders.seller"
    })
    Optional<Order> findWithDetailById(Long id);
}