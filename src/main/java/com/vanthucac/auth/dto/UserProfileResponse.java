package com.vanthucac.auth.dto;

import com.vanthucac.auth.entity.User;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String status,
        List<String> roles,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        var roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getStatus().name(),
                roles,
                user.getCreatedAt()
        );
    }
}