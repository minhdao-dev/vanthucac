package com.vanthucac.cart.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.cart.dto.AddCartItemRequest;
import com.vanthucac.cart.dto.CartItemResponse;
import com.vanthucac.cart.dto.CartResponse;
import com.vanthucac.cart.dto.UpdateCartItemRequest;
import com.vanthucac.cart.entity.Cart;
import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.cart.exception.CartErrorCode;
import com.vanthucac.cart.exception.CartException;
import com.vanthucac.cart.repository.CartRepository;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.seller.repository.SellerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    public CartService(
            CartRepository cartRepository,
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            UserRepository userRepository,
            SellerProfileRepository sellerProfileRepository
    ) {
        this.cartRepository = cartRepository;
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
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

        if (listing.getSeller() != null) {
            var isSelfPurchase = sellerProfileRepository.findByUserId(userId)
                    .map(profile -> profile.getId().equals(listing.getSeller().getId()))
                    .orElse(false);

            if (isSelfPurchase) {
                throw new CartException(
                        "You cannot purchase your own listing",
                        CartErrorCode.LISTING_NOT_AVAILABLE,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        var cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    var userRef = userRepository.getReferenceById(userId);
                    return Cart.create(userRef);
                });

        var currentQtyInCart = cart.findItemByListingId(listing.getId())
                .map(CartItem::getQuantity)
                .orElse(0);

        var totalQty = currentQtyInCart + request.quantity();

        if (listing.getStock() < totalQty) {
            throw new CartException(
                    "Requested quantity exceeds available stock (" + listing.getStock() + " available)",
                    CartErrorCode.LISTING_NOT_AVAILABLE,
                    HttpStatus.BAD_REQUEST
            );
        }

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

        if (item.getListing().getStock() < request.quantity()) {
            throw new CartException(
                    "Requested quantity exceeds available stock (" + item.getListing().getStock() + " available)",
                    CartErrorCode.LISTING_NOT_AVAILABLE,
                    HttpStatus.BAD_REQUEST
            );
        }

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
        var items = cart.getItems();

        if (items.isEmpty()) {
            return CartResponse.from(cart, List.of());
        }

        var listingIds = items.stream()
                .map(item -> item.getListing().getId())
                .toList();

        var imagesByListingId = listingImageRepository
                .findByListingIdInOrderBySortOrder(listingIds)
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getListing().getId(),
                        Collectors.mapping(ListingImage::getImageUrl, Collectors.toList())
                ));

        var itemResponses = items.stream()
                .map(item -> CartItemResponse.from(
                        item,
                        imagesByListingId.getOrDefault(item.getListing().getId(), List.of())
                ))
                .toList();

        return CartResponse.from(cart, itemResponses);
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}