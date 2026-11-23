package com.howie.pharmacy.pharmacy_store.filter;

import com.howie.pharmacy.pharmacy_store.utils.JwtUtils;
import com.howie.pharmacy.pharmacy_store.serviceImpl.TokenSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.howie.pharmacy.pharmacy_store.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenSessionService tokenSessionService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenSessionService tokenSessionService) {
        this.jwtUtils = jwtUtils;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 2. Kiểm tra token
            if (jwtUtils.validateToken(token)) {
                String sessionId = jwtUtils.getSessionIdFromToken(token);
                if (sessionId == null || tokenSessionService.isRevoked(sessionId)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String phone = jwtUtils.getPhoneFromToken(token);
                Integer userId = jwtUtils.getUserIdFromToken(token);
                UserPrincipal principal = new UserPrincipal(userId, phone);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                        null, new ArrayList<>());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
