package com.vanthucac.seller.controller;

import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.seller.dto.UpgradeSellerResponse;
import com.vanthucac.seller.dto.WalletResponse;
import com.vanthucac.seller.service.SellerService;
import com.vanthucac.user.dto.UpgradeSellerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UpgradeSellerResponse>> upgradeSeller(
            @Valid @RequestBody UpgradeSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var response = sellerService.upgradeSeller(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Seller profile created. Use newAccessToken for subsequent requests.",
                        response
                ));
    }

    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal Jwt jwt
    ) {
        var wallet = sellerService.getWallet(jwt);
        return ResponseEntity.ok(ApiResponse.ok("Wallet retrieved successfully", wallet));
    }
}