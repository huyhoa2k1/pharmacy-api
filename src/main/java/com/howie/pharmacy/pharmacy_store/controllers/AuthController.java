package com.howie.pharmacy.pharmacy_store.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.howie.pharmacy.pharmacy_store.dto.auth.AuthResponseDto;
import com.howie.pharmacy.pharmacy_store.dto.auth.LoginRequestDto;
import com.howie.pharmacy.pharmacy_store.dto.auth.RefreshTokenRequest;
import com.howie.pharmacy.pharmacy_store.dto.auth.RegisterRequestDto;
import com.howie.pharmacy.pharmacy_store.dto.user.UserDto;
import com.howie.pharmacy.pharmacy_store.entity.RefreshToken;
import com.howie.pharmacy.pharmacy_store.entity.User;
import com.howie.pharmacy.pharmacy_store.serviceImpl.AuthServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.LoginRateLimiterService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.RefreshTokenServiceImpl;
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

    public AuthController(AuthServiceImpl authService, RefreshTokenServiceImpl refreshTokenService, JwtUtils jwtUtils,
            LoginRateLimiterService loginRateLimiterService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
        this.loginRateLimiterService = loginRateLimiterService;
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
        return ResponseEntity.ok(authService.login(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", requestRefreshToken));
    }
}
