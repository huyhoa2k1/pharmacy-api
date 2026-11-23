package com.howie.pharmacy.pharmacy_store.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.howie.pharmacy.pharmacy_store.controllers.AuthController;
import com.howie.pharmacy.pharmacy_store.entity.RefreshToken;
import com.howie.pharmacy.pharmacy_store.entity.User;
import com.howie.pharmacy.pharmacy_store.filter.JwtAuthenticationFilter;
import com.howie.pharmacy.pharmacy_store.repository.RefreshTokenRepository;
import com.howie.pharmacy.pharmacy_store.repository.UserRepository;
import com.howie.pharmacy.pharmacy_store.security.AuthCookieService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.AuthServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.LoginRateLimiterService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.RefreshTokenServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.TokenSessionService;
import com.howie.pharmacy.pharmacy_store.utils.JwtUtils;

class AuthenticationRevocationTest {
    private final JwtUtils jwtUtils = new JwtUtils("test-secret-at-least-thirty-two-bytes");
    private final TokenSessionService sessions = mock(TokenSessionService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessTokenStopsAuthenticatingWhenItsSessionIsRevoked() throws Exception {
        User user = new User();
        user.setId(1);
        user.setPhone("123456789");
        String token = jwtUtils.generateToken(user, "session-id");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, sessions);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        when(sessions.isRevoked("session-id")).thenReturn(false, true);

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> assertNotNull(SecurityContextHolder.getContext().getAuthentication()));
        SecurityContextHolder.clearContext();
        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
    }

    @Test
    void logoutRevokesSessionBeforeDeletingRefreshToken() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(repository, mock(UserRepository.class), sessions);
        when(repository.findByToken("refresh")).thenReturn(Optional.of(new RefreshToken()));
        when(sessions.sessionId("refresh")).thenReturn("session-id");

        service.revoke("refresh");

        InOrder order = inOrder(sessions, repository);
        order.verify(sessions).revoke("session-id");
        order.verify(repository).deleteByTokenValue("refresh");
    }

    @Test
    void logoutDoesNotDeleteRefreshTokenWhenRedisIsUnavailable() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(repository, mock(UserRepository.class), sessions);
        when(repository.findByToken("refresh")).thenReturn(Optional.of(new RefreshToken()));
        when(sessions.sessionId("refresh")).thenReturn("session-id");
        doThrow(new IllegalStateException("Redis unavailable")).when(sessions).revoke("session-id");

        assertThrows(IllegalStateException.class, () -> service.revoke("refresh"));
        verify(repository, never()).deleteByTokenValue("refresh");
    }

    @Test
    void refreshRejectsRevokedSessionEvenIfDatabaseStillContainsToken() {
        RefreshTokenServiceImpl refreshService = mock(RefreshTokenServiceImpl.class);
        AuthController controller = new AuthController(mock(AuthServiceImpl.class), refreshService, jwtUtils,
                mock(LoginRateLimiterService.class), sessions, new AuthCookieService(true, "Lax", 3));
        when(sessions.sessionId("refresh")).thenReturn("session-id");
        when(sessions.isRevoked("session-id")).thenReturn(true);

        assertEquals(401, controller.refreshToken("refresh").getStatusCode().value());
    }

    @Test
    void newLoginRevokesThePreviousSession() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(repository, users, sessions);
        User user = new User();
        RefreshToken previous = new RefreshToken();
        previous.setToken("previous-refresh");
        previous.setExpiryDate(Instant.now().plusSeconds(60));
        when(users.findById(1)).thenReturn(Optional.of(user));
        when(repository.findByUser(user)).thenReturn(Optional.of(previous));
        when(sessions.sessionId("previous-refresh")).thenReturn("previous-session");
        when(repository.save(previous)).thenReturn(previous);

        service.createRefreshToken(1);

        verify(sessions).revoke("previous-session");
    }
}