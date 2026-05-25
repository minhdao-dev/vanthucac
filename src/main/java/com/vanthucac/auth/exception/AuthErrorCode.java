package com.vanthucac.auth.exception;

public final class AuthErrorCode {
    private AuthErrorCode() {
    }

    public static final String EMAIL_ALREADY_EXISTS = "AUTH_EMAIL_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String ACCOUNT_DISABLED = "AUTH_ACCOUNT_DISABLED";
    public static final String REFRESH_TOKEN_INVALID = "AUTH_REFRESH_TOKEN_INVALID";
    public static final String REFRESH_TOKEN_EXPIRED = "AUTH_REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REUSED = "AUTH_REFRESH_TOKEN_REUSED";
}