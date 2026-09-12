package com.howie.pharmacy.pharmacy_store.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Logs method, path, response status and duration of every API call
@Component
@Order(1)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger API_LOGGER = LoggerFactory.getLogger("API");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String path = queryString != null ? uri + "?" + queryString : uri;

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                API_LOGGER.error("API {} {} -> {} ({}ms)", method, path, status, duration);
            } else if (status >= 400) {
                API_LOGGER.warn("API {} {} -> {} ({}ms)", method, path, status, duration);
            } else {
                API_LOGGER.info("API {} {} -> {} ({}ms)", method, path, status, duration);
            }
        }
    }
}
