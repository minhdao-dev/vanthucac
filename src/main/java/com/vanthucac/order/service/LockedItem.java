package com.vanthucac.order.service;

import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.listing.entity.BookListing;

public record LockedItem(
        CartItem cartItem,
        BookListing listing) {
}