package com.vanthucac.user.service;

import com.vanthucac.auth.dto.UserProfileResponse;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.user.dto.UpdateProfileRequest;
import com.vanthucac.user.exception.UserException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
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

    private User getUserFromJwt(Jwt jwt) {
        var userId = Long.parseLong(jwt.getSubject());
        return userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);
    }
}