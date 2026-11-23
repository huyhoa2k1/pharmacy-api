package com.howie.pharmacy.pharmacy_store.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import com.howie.pharmacy.pharmacy_store.config.SecurityConfig;
import com.howie.pharmacy.pharmacy_store.dto.auth.AuthResponseDto;
import com.howie.pharmacy.pharmacy_store.filter.JwtAuthenticationFilter;
import com.howie.pharmacy.pharmacy_store.security.AuthCookieService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.AuthServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.LoginRateLimiterService;
import com.howie.pharmacy.pharmacy_store.serviceImpl.RefreshTokenServiceImpl;
import com.howie.pharmacy.pharmacy_store.serviceImpl.TokenSessionService;
import com.howie.pharmacy.pharmacy_store.utils.JwtUtils;

@WebMvcTest(AuthController.class)
@Import({ AuthController.class, SecurityConfig.class, JwtAuthenticationFilter.class, AuthCookieService.class })
class AuthCookieCsrfTest {
    // Avoids loading the main application class, whose @EnableJpaAuditing needs a
    // JPA context.
    @SpringBootConfiguration
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthServiceImpl authService;
    @MockitoBean
    private RefreshTokenServiceImpl refreshTokenService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private LoginRateLimiterService loginRateLimiterService;
    @MockitoBean
    private TokenSessionService tokenSessionService;

    @Test
    void logoutWithoutCsrfTokenIsRejectedAndDoesNotRevoke() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", "refresh")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refreshWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "refresh")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithCsrfTokenRevokesAndClearsCookie() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN")).andReturn();
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(post("/api/auth/logout").cookie(csrfCookie, new Cookie("refreshToken", "refresh"))
                .header("X-XSRF-TOKEN", token))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=;")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
        verify(refreshTokenService).revoke("refresh");
    }

    @Test
    void loginSetsHttpOnlyRefreshCookieAndHidesTokenFromBody() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponseDto("access", "refresh", null));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0123456789\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Secure")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));
    }
}
