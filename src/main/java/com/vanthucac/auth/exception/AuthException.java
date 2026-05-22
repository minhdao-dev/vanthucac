package com.vanthucac.auth.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuthException extends BusinessException {

    public AuthException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static AuthException emailAlreadyExists(String email) {
        return new AuthException(
                "Email already exists: " + email,
                AuthErrorCode.EMAIL_ALREADY_EXISTS,
                HttpStatus.CONFLICT
        );
    }

    public static AuthException invalidCredentials() {
        return new AuthException(
                "Invalid email or password",
                AuthErrorCode.INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED
        );
    }

    public static AuthException accountDisabled() {
        return new AuthException(
                "Account is disabled",
                AuthErrorCode.ACCOUNT_DISABLED,
                HttpStatus.FORBIDDEN
        );
    }

    public static AuthException refreshTokenInvalid() {
        return new AuthException(
                "Refresh token is invalid or expired",
                AuthErrorCode.REFRESH_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED
        );
    }

    public static AuthException refreshTokenReused() {
        return new AuthException(
                "Refresh token reuse detected, all sessions revoked",
                AuthErrorCode.REFRESH_TOKEN_REUSED,
                HttpStatus.UNAUTHORIZED
        );
    }
}