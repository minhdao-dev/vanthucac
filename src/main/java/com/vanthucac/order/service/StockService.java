package com.vanthucac.order.service;

import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.order.entity.Order;
import com.vanthucac.order.entity.OrderItem;
import com.vanthucac.order.exception.OrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final BookListingRepository bookListingRepository;

    public StockService(BookListingRepository bookListingRepository) {
        this.bookListingRepository = bookListingRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<LockedItem> lockAndDeduct(List<CartItem> cartItems) {
        var sorted = cartItems.stream()
                .sorted(Comparator.comparingLong(item -> item.getListing().getId()))
                .toList();

        var result = new ArrayList<LockedItem>();

        for (var cartItem : sorted) {
            var listing = bookListingRepository
                    .findByIdWithLock(cartItem.getListing().getId())
                    .orElseThrow(() -> OrderException.stockInsufficient(
                            cartItem.getListing().getBookCatalog().getTitle()));

            if (listing.getStatus() != BookListing.ListingStatus.ACTIVE) {
                throw OrderException.stockInsufficient(listing.getBookCatalog().getTitle());
            }

            if (listing.getStock() < cartItem.getQuantity()) {
                throw OrderException.stockInsufficient(listing.getBookCatalog().getTitle());
            }

            listing.deductStock(cartItem.getQuantity());
            result.add(new LockedItem(cartItem, listing));
        }

        return result;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restore(Order order) {
        List<OrderItem> allItems = order.isParentOrder()
                ? order.getSubOrders().stream()
                .flatMap(sub -> sub.getItems().stream())
                .toList()
                : new ArrayList<>(order.getItems());

        if (allItems.isEmpty()) return;

        var sorted = allItems.stream()
                .sorted(Comparator.comparingLong(item -> item.getListing().getId()))
                .toList();

        for (var item : sorted) {
            var listing = bookListingRepository
                    .findByIdWithLock(item.getListing().getId())
                    .orElseThrow(ListingException::listingNotFound);

            listing.restoreStock(item.getQuantity());
            log.debug("Stock restored for listing {} — quantity +{}", listing.getId(), item.getQuantity());
        }
    }
}