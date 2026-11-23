package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.auth.AuthResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.auth.LoginRequestDto;
import com.howie.pharmacy.pharmacy_store.dto.auth.RegisterRequestDto;
import com.howie.pharmacy.pharmacy_store.dto.user.UserDto;
import com.howie.pharmacy.pharmacy_store.entity.RefreshToken;
import com.howie.pharmacy.pharmacy_store.entity.User;
import com.howie.pharmacy.pharmacy_store.security.AuthCookieService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.AuthServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.LoginRateLimiterService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.RefreshTokenServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.TokenSessionService;
import com.howie.pharmacy.pharmacy_store.utils.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthServiceImpl authService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final JwtUtils jwtUtils;
    private final LoginRateLimiterService loginRateLimiterService;
    private final TokenSessionService tokenSessionService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthServiceImpl authService, RefreshTokenServiceImpl refreshTokenService, JwtUtils jwtUtils,
            LoginRateLimiterService loginRateLimiterService, TokenSessionService tokenSessionService,
            AuthCookieService authCookieService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
        this.loginRateLimiterService = loginRateLimiterService;
        this.tokenSessionService = tokenSessionService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest) {
        loginRateLimiterService.checkRegisterIpLimit(getClientIp(httpRequest));
        System.out.println("Received registration request: " + request);
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        loginRateLimiterService.checkIpLimit(getClientIp(httpRequest));
        AuthResponseDto auth = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        authCookieService.createRefreshCookie(auth.getRefreshToken()).toString())
                .body(new AuthResponseDto(auth.getAccessToken(), null, auth.getUser()));
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken());
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String requestRefreshToken) {
        if (requestRefreshToken == null || requestRefreshToken.isBlank()
                || tokenSessionService.isRevoked(tokenSessionService.sessionId(requestRefreshToken))) {
            return ResponseEntity.status(401).build();
        }

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken).orElse(null);
        if (refreshToken == null || refreshToken.getExpiryDate().isBefore(java.time.Instant.now())) {
            return ResponseEntity.status(401).build();
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateToken(user, tokenSessionService.sessionId(requestRefreshToken));

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshCookie().toString())
                .build();
    }
}
