package com.vanthucac.auth.service;

import com.vanthucac.auth.dto.LoginRequest;
import com.vanthucac.auth.dto.LogoutRequest;
import com.vanthucac.auth.dto.RefreshRequest;
import com.vanthucac.auth.dto.RegisterRequest;
import com.vanthucac.auth.dto.TokenResponse;
import com.vanthucac.auth.dto.UserProfileResponse;
import com.vanthucac.auth.entity.Role;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.exception.AuthException;
import com.vanthucac.auth.repository.RoleRepository;
import com.vanthucac.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RedisTokenService redisTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            RedisTokenService redisTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.redisTokenService = redisTokenService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AuthException.emailAlreadyExists(request.email());
        }

        var passwordHash = passwordEncoder.encode(request.password());
        var user = User.create(request.email(), passwordHash, request.fullName());

        if (request.phone() != null && !request.phone().isBlank()) {
            user.updatePhone(request.phone());
        }

        var buyerRole = roleRepository.findByName(Role.RoleName.BUYER)
                .orElseThrow(() -> new IllegalStateException("Role BUYER not found — check V1 migration"));

        user.addRole(buyerRole);
        userRepository.save(user);

        return UserProfileResponse.from(user);
    }

    public TokenResponse login(LoginRequest request, String deviceInfo) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(AuthException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw AuthException.accountDisabled();
        }

        return issueTokenPair(user, deviceInfo);
    }

    public TokenResponse refresh(RefreshRequest request) {
        var oldRawToken = request.refreshToken();
        var oldTokenHash = tokenService.hashToken(oldRawToken);

        var activeToken = redisTokenService.findActiveToken(oldTokenHash);
        if (activeToken.isPresent()) {
            var data = activeToken.get();
            var user = userRepository.findById(Long.parseLong(data.userId()))
                    .orElseThrow(AuthException::refreshTokenInvalid);

            var newRawToken = tokenService.generateRefreshToken();
            var newTokenHash = tokenService.hashToken(newRawToken);
            var newAccessToken = tokenService.generateAccessToken(user, data.sessionId());

            var rotated = redisTokenService.rotateRefreshToken(
                    oldTokenHash,
                    newTokenHash,
                    user.getId(),
                    data.sessionId(),
                    data.familyId()
            );

            if (!rotated) {
                redisTokenService.revokeFamilyAndSession(
                        data.familyId(),
                        data.sessionId(),
                        user.getId()
                );
                throw AuthException.refreshTokenReused();
            }

            return TokenResponse.of(newAccessToken, newRawToken);
        }

        if (redisTokenService.isTokenUsed(oldTokenHash)) {
            throw AuthException.refreshTokenReused();
        }

        throw AuthException.refreshTokenInvalid();
    }

    public void logout(LogoutRequest request, Jwt jwt) {
        var tokenHash = tokenService.hashToken(request.refreshToken());
        var activeToken = redisTokenService.findActiveToken(tokenHash);

        if (activeToken.isEmpty()) return;

        var data = activeToken.get();
        var jwtUserId = jwt.getSubject();

        if (!data.userId().equals(jwtUserId)) {
            throw AuthException.refreshTokenInvalid();
        }

        var jwtSessionId = jwt.getClaim("sessionId").toString();
        if (!data.sessionId().equals(jwtSessionId)) {
            throw AuthException.refreshTokenInvalid();
        }

        redisTokenService.revokeSession(
                data.sessionId(),
                tokenHash,
                data.familyId(),
                Long.parseLong(data.userId())
        );
    }

    public void logoutAll(Jwt jwt) {
        var userId = Long.parseLong(jwt.getSubject());
        redisTokenService.revokeAllSessions(userId);
    }

    private TokenResponse issueTokenPair(User user, String deviceInfo) {
        var sessionId = UUID.randomUUID().toString();
        var familyId = UUID.randomUUID().toString();

        var accessToken = tokenService.generateAccessToken(user, sessionId);
        var rawRefreshToken = tokenService.generateRefreshToken();
        var tokenHash = tokenService.hashToken(rawRefreshToken);

        redisTokenService.saveRefreshToken(
                tokenHash,
                user.getId(),
                sessionId,
                familyId,
                deviceInfo
        );

        return TokenResponse.of(accessToken, rawRefreshToken);
    }
}