package com.vanthucac.cart.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.dto.AddCartItemRequest;
import com.vanthucac.cart.dto.CartItemResponse;
import com.vanthucac.cart.dto.CartResponse;
import com.vanthucac.cart.dto.UpdateCartItemRequest;
import com.vanthucac.cart.entity.Cart;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Jwt jwt) {
        var userId = extractUserId(jwt);
        return cartRepository.findByUserId(userId)
                .map(this::toCartResponse)
                .orElse(CartResponse.empty());
    }

    @Transactional
    public CartResponse addItem(AddCartItemRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);

        var listing = bookListingRepository.findById(request.listingId())
                .orElseThrow(ListingException::listingNotFound);

        if (listing.getStatus() != BookListing.ListingStatus.ACTIVE) {
            throw CartException.listingNotAvailable();
        }

        var cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    var userRef = userRepository.getReferenceById(userId);
                    return Cart.create(userRef);
                });

        cart.addOrMergeItem(listing, request.quantity());
        cartRepository.save(cart);

        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);

        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(CartException::cartNotFound);

        var item = cart.findItemById(itemId)
                .orElseThrow(CartException::itemNotFound);

        item.updateQuantity(request.quantity());
        cart.touch();

        return toCartResponse(cart);
    }

    @Transactional
    public void removeItem(Long itemId, Jwt jwt) {
        var userId = extractUserId(jwt);

        var cart = cartRepository.findByUserId(userId)
                .orElseThrow(CartException::cartNotFound);

        var item = cart.findItemById(itemId)
                .orElseThrow(CartException::itemNotFound);

        cart.getItems().remove(item);
        cart.touch();
    }

    private CartResponse toCartResponse(Cart cart) {
        var itemResponses = cart.getItems().stream()
                .map(item -> {
                    var images = listingImageRepository
                            .findByListingIdOrderBySortOrder(item.getListing().getId())
                            .stream()
                            .map(ListingImage::getImageUrl)
                            .toList();
                    return CartItemResponse.from(item, images);
                })
                .toList();

        return CartResponse.from(cart, itemResponses);
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}