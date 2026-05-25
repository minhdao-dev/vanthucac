package com.vanthucac.listing.controller;

import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.listing.dto.CreateListingRequest;
import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.listing.dto.UpdateListingRequest;
import com.vanthucac.listing.service.BookListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/listings")
public class BookListingController {

    private final BookListingService bookListingService;

    public BookListingController(BookListingService bookListingService) {
        this.bookListingService = bookListingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ListingResponse>>> search(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var result = bookListingService.search(
                bookId, sellerId, listingType, condition,
                minPrice, maxPrice, page, size, sort
        );
        return ResponseEntity.ok(ApiResponse.ok("Listings retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> getById(
            @PathVariable Long id
    ) {
        var listing = bookListingService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Listing retrieved successfully", listing));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> create(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var listing = bookListingService.create(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Listing created successfully", listing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var listing = bookListingService.update(id, request, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Listing updated successfully", listing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        bookListingService.deactivate(id, jwt);
        return ResponseEntity.noContent().build();
    }
}