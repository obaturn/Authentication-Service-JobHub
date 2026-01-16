package com.example.Authentication_System;

import com.example.Authentication_System.Controllers.AuthController;
import com.example.Authentication_System.Domain.model.*;
import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.UserUseCase;
import com.example.Authentication_System.Security.JwtUtils;
import com.example.Authentication_System.Services.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @MockitoBean
    private RateLimitingService rateLimitingService;

    @TestConfiguration
    static class AuthControllerTestConfiguration {
        @Bean
        public UserUseCase userUseCase() {
            return Mockito.mock(UserUseCase.class);
        }

        @Bean
        public JwtUtils jwtUtils() {
            return Mockito.mock(JwtUtils.class);
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return Mockito.mock(UserDetailsService.class);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserUseCase userUseCase;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User requestUser;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder().build();
        testUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("hashedPassword")
                .userType("job_seeker")
                .status("active")
                .emailVerified(false)
                .mfaEnabled(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        testUser.setUserProfile(profile);

        requestUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("StrongPassword123!")
                .userType("job_seeker")
                .build();
        requestUser.setUserProfile(UserProfile.builder().build());
    }

    @Test
    void register_WithValidData_ShouldReturnOk() throws Exception {
        when(userUseCase.register(any(User.class), any(), any())).thenReturn(testUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.firstName").value(testUser.getFirstName()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void register_WithExistingEmail_ShouldReturnBadRequest() throws Exception {
        when(userUseCase.register(any(User.class), any(), any()))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void sendVerificationEmail_ShouldReturnOk() throws Exception {
        doNothing().when(userUseCase).sendVerificationEmail(anyString());
        mockMvc.perform(post("/api/auth/send-verification-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void verifyEmail_WithValidToken_ShouldReturnOk() throws Exception {
        when(userUseCase.verifyEmail("valid-token")).thenReturn(true);
        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified successfully."));
    }

    @Test
    void forgotPassword_ShouldReturnOk() throws Exception {
        doNothing().when(userUseCase).forgotPassword(anyString());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_ShouldReturnOk() throws Exception {
        doNothing().when(userUseCase).resetPassword(anyString(), anyString());
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "valid-token", "newPassword", "newPass123!"))))
                .andExpect(status().isOk());
    }

    @Test
    void getProfile_ShouldReturnUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfile userProfile = UserProfile.builder().id(userId).bio("A bio").build();
        when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(userId.toString());
        when(userUseCase.getProfile(userId)).thenReturn(Optional.of(userProfile));

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.bio").value("A bio"));
    }

    @Test
    void setupMfa_ShouldReturnMfaDetails() throws Exception {
        UUID userId = UUID.randomUUID();
        MfaSetupResponse mfaSetupResponse = new MfaSetupResponse("secret", "qr-code");
        when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(userId.toString());
        when(userUseCase.setupMfa(userId)).thenReturn(mfaSetupResponse);

        mockMvc.perform(post("/api/auth/mfa/setup")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("secret"))
                .andExpect(jsonPath("$.qrCode").value("qr-code"));
    }

    @Test
    void enableMfa_ShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtUtils.getUserIdFromToken(anyString())).thenReturn(userId.toString());
        doNothing().when(userUseCase).enableMfa(userId, "123456");

        mockMvc.perform(post("/api/auth/mfa/enable")
                        .header("Authorization", "Bearer some-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "123456"))))
                .andExpect(status().isOk());
    }

    @Test
    void verifyMfa_ShouldReturnAuthResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder().accessToken("access-token").build();
        when(userUseCase.verifyMfa("mfa-token", "123456")).thenReturn(Optional.of(authResponse));

        mockMvc.perform(post("/api/auth/login/mfa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mfaToken", "mfa-token", "code", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }
}