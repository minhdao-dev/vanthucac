package com.vanthucac.user.service;

import com.vanthucac.auth.dto.UserProfileResponse;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.seller.dto.SellerProfileResponse;
import com.vanthucac.seller.entity.SellerProfile;
import com.vanthucac.seller.exception.SellerException;
import com.vanthucac.seller.repository.SellerProfileRepository;
import com.vanthucac.user.dto.UpdateProfileRequest;
import com.vanthucac.user.dto.UpgradeSellerRequest;
import com.vanthucac.user.exception.UserException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    public UserService(
            UserRepository userRepository,
            SellerProfileRepository sellerProfileRepository
    ) {
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
    }

    public UserProfileResponse getProfile(Jwt jwt) {
        var user = getUserFromJwt(jwt);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request, Jwt jwt) {
        var user = getUserFromJwt(jwt);
        user.updateProfile(request.fullName(), request.phone(), request.avatarUrl());
        return UserProfileResponse.from(user);
    }

    @Transactional
    public SellerProfileResponse upgradeSeller(UpgradeSellerRequest request, Jwt jwt) {
        var user = getUserFromJwt(jwt);

        if (sellerProfileRepository.existsByUser(user)) {
            throw SellerException.alreadySeller();
        }

        var sellerProfile = SellerProfile.create(user, request.shopName(), request.description());
        sellerProfileRepository.save(sellerProfile);

        return SellerProfileResponse.from(sellerProfile);
    }

    private User getUserFromJwt(Jwt jwt) {
        var userId = Long.parseLong(jwt.getSubject());
        return userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);
    }
}