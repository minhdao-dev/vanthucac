package com.vanthucac.listing.controller;

import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.listing.service.AdminListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
        var response = adminListingService.getPendingListings(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Pending listings retrieved successfully", response));
    }

    @PutMapping("/{listingId}/approve")
    public ResponseEntity<ApiResponse<ListingResponse>> approveListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var response = adminListingService.approveListing(listingId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Listing approved successfully", response));
    }

    @PutMapping("/{listingId}/reject")
    public ResponseEntity<ApiResponse<ListingResponse>> rejectListing(
            @PathVariable Long listingId,
            @Valid @RequestBody RejectListingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var response = adminListingService.rejectListing(listingId, request.reason(), jwt);
        return ResponseEntity.ok(ApiResponse.ok("Listing rejected successfully", response));
    }

    public record RejectListingRequest(
            @NotBlank
            String reason
    ) {
    }
}