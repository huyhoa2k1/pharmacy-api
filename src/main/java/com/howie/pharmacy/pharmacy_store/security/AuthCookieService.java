package com.howie.pharmacy.pharmacy_store.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {
    public static final String REFRESH_COOKIE = "refreshToken";
    // Refresh token is only sent to the auth endpoints.
    public static final String AUTH_PATH = "/api/auth";

    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public AuthCookieService(@Value("${pharmacy.auth.cookie.secure:true}") boolean secure,
            @Value("${pharmacy.auth.cookie.same-site:Lax}") String sameSite,
            @Value("${pharmacy.auth.refresh-token-days:3}") long refreshTokenDays) {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requires a secure cookie");
        }
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofDays(refreshTokenDays);
    }

    public boolean isSecure() {
        return secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public ResponseCookie createRefreshCookie(String token) {
        return build(token, maxAge);
    }

    public ResponseCookie clearRefreshCookie() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration age) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(AUTH_PATH)
                .maxAge(age)
                .build();
    }
}
