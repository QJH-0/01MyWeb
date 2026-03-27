package com.myweb.backend.common;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_INVALID_REFRESH_TOKEN = "AUTH_INVALID_REFRESH_TOKEN";
    public static final String AUTH_CAPTCHA_INVALID = "AUTH_CAPTCHA_INVALID";
    public static final String AUTH_WEAK_PASSWORD = "AUTH_WEAK_PASSWORD";
    public static final String AUTH_USERNAME_EXISTS = "AUTH_USERNAME_EXISTS";
    public static final String AUTH_REGISTER_RATE_LIMITED = "AUTH_REGISTER_RATE_LIMITED";
    public static final String AUTH_LOGIN_RATE_LIMITED = "AUTH_LOGIN_RATE_LIMITED";
}
