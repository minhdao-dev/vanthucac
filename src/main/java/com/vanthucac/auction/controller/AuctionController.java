package com.vanthucac.auction.controller;

import com.vanthucac.auction.dto.*;
import com.vanthucac.auction.service.AuctionService;
import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping("/auction-sessions")
    public ResponseEntity<ApiResponse<AuctionSessionResponse>> createSession(
            @Valid @RequestBody CreateAuctionSessionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var session = auctionService.createSession(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Auction session created successfully", session));
    }

    @GetMapping("/auction-sessions")
    public ResponseEntity<ApiResponse<PageResponse<AuctionSessionResponse>>> getSessions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        var sessions = auctionService.getSessions(status, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Sessions retrieved successfully", sessions));
    }

    @GetMapping("/auction-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<AuctionSessionResponse>> getSessionById(
            @PathVariable Long sessionId
    ) {
        var session = auctionService.getSessionById(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Session retrieved successfully", session));
    }

    @PostMapping("/auction-sessions/{sessionId}/items")
    public ResponseEntity<ApiResponse<AuctionItemResponse>> addItem(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateAuctionItemRequest request
    ) {
        var item = auctionService.addItem(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Auction item added successfully", item));
    }

    @PostMapping("/auction-items/{itemId}/bids")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(
            @PathVariable Long itemId,
            @Valid @RequestBody PlaceBidRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var bid = auctionService.placeBid(itemId, request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bid placed successfully", bid));
    }

    @GetMapping("/auction-items/{itemId}/bids")
    public ResponseEntity<ApiResponse<PageResponse<BidResponse>>> getBidHistory(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var bids = auctionService.getBidHistory(itemId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Bid history retrieved successfully", bids));
    }
}