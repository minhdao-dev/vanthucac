package com.vanthucac.listing.controller;

import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.listing.dto.ReviewListingRequest;
import com.vanthucac.listing.service.AdminListingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/listings")
public class AdminListingController {

    private final AdminListingService adminListingService;

    public AdminListingController(AdminListingService adminListingService) {
        this.adminListingService = adminListingService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<PageResponse<ListingResponse>>> getPendingListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var listings = adminListingService.getPendingListings(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Pending listings retrieved", listings));
    }

    @PutMapping("/{listingId}/approve")
    public ResponseEntity<ApiResponse<ListingResponse>> approveListing(
            @PathVariable Long listingId
    ) {
        var listing = adminListingService.approveListing(listingId);
        return ResponseEntity.ok(ApiResponse.ok("Listing approved", listing));
    }

    @PutMapping("/{listingId}/reject")
    public ResponseEntity<ApiResponse<ListingResponse>> rejectListing(
            @PathVariable Long listingId,
            @Valid @RequestBody ReviewListingRequest request
    ) {
        var listing = adminListingService.rejectListing(listingId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Listing rejected", listing));
    }
}