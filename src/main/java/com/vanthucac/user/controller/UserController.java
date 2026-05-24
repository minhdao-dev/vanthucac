package com.vanthucac.user.controller;

import com.vanthucac.auth.dto.UserProfileResponse;
import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.seller.dto.SellerProfileResponse;
import com.vanthucac.user.dto.UpdateProfileRequest;
import com.vanthucac.user.dto.UpgradeSellerRequest;
import com.vanthucac.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        var profile = userService.getProfile(jwt);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var profile = userService.updateProfile(request, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", profile));
    }

    @PostMapping("/me/seller")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> upgradeSeller(
            @Valid @RequestBody UpgradeSellerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var sellerProfile = userService.upgradeSeller(request, jwt);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seller profile created successfully", sellerProfile));
    }
}