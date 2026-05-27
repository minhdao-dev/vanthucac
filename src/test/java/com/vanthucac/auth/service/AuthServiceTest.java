package com.vanthucac.auth.service;

import com.vanthucac.auth.dto.LoginRequest;
import com.vanthucac.auth.dto.RefreshRequest;
import com.vanthucac.auth.dto.RegisterRequest;
import com.vanthucac.auth.entity.Role;
import com.vanthucac.auth.entity.User;
import com.vanthucac.auth.exception.AuthException;
import com.vanthucac.auth.repository.RoleRepository;
import com.vanthucac.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TokenService tokenService;
    @Mock
    RedisTokenService redisTokenService;

    @InjectMocks
    AuthService authService;

    private User activeUser;
    private Role buyerRole;

    @BeforeEach
    void setUp() {
        buyerRole = mock(Role.class);
        activeUser = User.create("user@example.com", "hashed-password", "Test User");
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        void register_success() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("hashed");
            given(roleRepository.findByName(Role.RoleName.BUYER)).willReturn(Optional.of(buyerRole));
            given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = authService.register(
                    new RegisterRequest("new@example.com", "Password@123", "New User", null));

            assertThat(result.email()).isEqualTo("new@example.com");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        void register_throwsConflict_whenEmailExists() {
            given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(
                    new RegisterRequest("existing@example.com", "Password@123", "Name", null)))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Email already exists");
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        void login_success() {
            given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches("Password@123", "hashed-password")).willReturn(true);
            given(tokenService.generateAccessToken(any(), anyString())).willReturn("access-token");
            given(tokenService.generateRefreshToken()).willReturn("refresh-token");
            given(tokenService.hashToken(anyString())).willReturn("token-hash");

            var result = authService.login(
                    new LoginRequest("user@example.com", "Password@123"), "Mozilla");

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            then(redisTokenService).should()
                    .saveRefreshToken(anyString(), any(), anyString(), anyString(), anyString());
        }

        @Test
        void login_throwsUnauthorized_whenUserNotFound() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("wrong@example.com", "pass"), null))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void login_throwsUnauthorized_whenPasswordWrong() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("user@example.com", "wrong"), null))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        void login_throwsForbidden_whenAccountBanned() {
            var bannedUser = User.create("banned@example.com", "hash", "Banned");
            setField(bannedUser, "status", User.UserStatus.BANNED);

            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(bannedUser));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("banned@example.com", "pass"), null))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Account is disabled");
        }
    }

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        void refresh_success_rotatesToken() {
            var tokenData = new RedisTokenService.RefreshTokenData("1", "session-id", "family-id");

            given(tokenService.hashToken("old-token")).willReturn("old-hash");
            given(redisTokenService.findActiveToken("old-hash")).willReturn(Optional.of(tokenData));
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(tokenService.generateRefreshToken()).willReturn("new-token");
            given(tokenService.hashToken("new-token")).willReturn("new-hash");
            given(tokenService.generateAccessToken(any(), anyString())).willReturn("new-access");
            given(redisTokenService.rotateRefreshToken(
                    "old-hash", "new-hash", 1L, "session-id", "family-id"
            )).willReturn(true);

            var result = authService.refresh(new RefreshRequest("old-token"));

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-token");
        }

        @Test
        void refresh_revokesFamily_whenReuseDetected() {
            given(tokenService.hashToken("reused-token")).willReturn("reused-hash");
            given(redisTokenService.findActiveToken("reused-hash")).willReturn(Optional.empty());
            given(redisTokenService.isTokenUsed("reused-hash")).willReturn(true);

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("reused-token")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("reuse detected");
        }

        @Test
        void refresh_throwsUnauthorized_whenTokenNotFound() {
            given(tokenService.hashToken("fake-token")).willReturn("fake-hash");
            given(redisTokenService.findActiveToken("fake-hash")).willReturn(Optional.empty());
            given(redisTokenService.isTokenUsed("fake-hash")).willReturn(false);

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("fake-token")))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("invalid or expired");
        }

        @Test
        void refresh_revokesFamily_whenLuaScriptFails() {
            var tokenData = new RedisTokenService.RefreshTokenData("1", "session-id", "family-id");

            given(tokenService.hashToken("race-token")).willReturn("race-hash");
            given(redisTokenService.findActiveToken("race-hash")).willReturn(Optional.of(tokenData));
            given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
            given(tokenService.generateRefreshToken()).willReturn("new-token");
            given(tokenService.hashToken("new-token")).willReturn("new-hash");
            given(tokenService.generateAccessToken(any(), anyString())).willReturn("new-access");
            given(redisTokenService.rotateRefreshToken(any(), any(), any(), any(), any()))
                    .willReturn(false);

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("race-token")))
                    .isInstanceOf(AuthException.class);

            then(redisTokenService).should()
                    .revokeFamilyAndSession("family-id", "session-id", 1L);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}