package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.AuthResponse;
import com.example.Authentication_System.Domain.model.RefreshToken;
import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.RefreshTokenRepository;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Security.JwtUtils;
import com.example.Authentication_System.Services.UserServiceImplementations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//@SuppressWarnings(
//@SpringBootTest(properties = {
//    "spring.flyway.enabled=false",
//    "spring.jpa.hibernate.ddl-auto=create-drop",
//    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
//    "spring.datasource.driver-class-name=org.h2.Driver",
//    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//    "spring.jpa.show-sql=false",
//    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
//})
@ExtendWith(MockitoExtension.class)
class UserServiceApplicationTests {

    @InjectMocks
    private UserServiceImplementations userService;


    @Mock
    private UserRepository userRepository;


    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;


    @Mock
    private JwtUtils jwtUtils;

	@Test

	   void register_Successful() {
	       User inputUser = User.builder()
	               .firstName("John")
	               .lastName("Doe")
	               .email("john@example.com")
	               .passwordHash("password123")
	               .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-pass");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .userType("job_seeker")
                .status("active")
                .emailVerified(false)
                .mfaEnabled(false)
                .mfaSecret(null)
                .googleId(null)
                .avatarUrl(null)
                .phone(null)
                .location(null)
                .bio(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(null)
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User registered = userService.register(inputUser);

        assertNotNull(registered);
        assertNotNull(registered.getId());
        assertEquals("hashed-pass", registered.getPasswordHash());
        assertEquals("John", registered.getFirstName());
        assertEquals("Doe", registered.getLastName());
        assertEquals("john@example.com", registered.getEmail());
        assertEquals("job_seeker", registered.getUserType());
        assertEquals("active", registered.getStatus());
        assertNotNull(registered.getCreatedAt());
        assertNotNull(registered.getUpdatedAt());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void refreshToken_Successful() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("john@example.com")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("hashed-token")
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();

        when(passwordEncoder.encode("refresh-token"))
                .thenReturn("hashed-token");

        when(passwordEncoder.encode("new-refresh-token"))
                .thenReturn("new-hashed-token");

        when(refreshTokenRepository.findByTokenHash("hashed-token"))
                .thenReturn(Optional.of(refreshToken));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(jwtUtils.generateAccessToken(user))
                .thenReturn("new-access-token");

        when(jwtUtils.generateRefreshToken(user))
                .thenReturn("new-refresh-token");

        AuthResponse result = userService.refreshToken("refresh-token");

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(new User()));

        User inputUser = User.builder()
                .email("john@example.com")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                userService.register(inputUser));
    }


    @Test
    void login_Successful() {
        User existing = User.builder()
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("password123", "hashed-pass"))
                .thenReturn(true);

        when(jwtUtils.generateAccessToken(existing))
                .thenReturn("access-token");

        when(jwtUtils.generateRefreshToken(existing))
                .thenReturn("refresh-token");

        Optional<AuthResponse> result =
                userService.login("john@example.com", "password123");

        assertTrue(result.isPresent());
        assertEquals("access-token", result.get().getAccessToken());
        assertEquals("refresh-token", result.get().getRefreshToken());
    }

    @Test
    void login_WrongPassword_ReturnsEmpty() {
        User existing = User.builder()
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("wrong", "hashed-pass"))
                .thenReturn(false);

        Optional<AuthResponse> result =
                userService.login("john@example.com", "wrong");

        assertTrue(result.isEmpty());
    }

    @Test
    void login_UserNotFound_ReturnsEmpty() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        Optional<AuthResponse> result =
                userService.login("unknown@example.com", "pass");

        assertTrue(result.isEmpty());
    }


    @Test
    void findByEmail_ReturnsUser() {
        User user = new User();
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("john@example.com");

        assertTrue(result.isPresent());
    }


    @Test
    void findById_ReturnsUser() {
        UUID id = UUID.randomUUID();
        User user = new User();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(id);

        assertTrue(result.isPresent());
    }


    @Test
    void updateProfile_Successful() {
        UUID id = UUID.randomUUID();
        User existing = User.builder()
                .id(id)
                .firstName("Old Name")
                .email("john@example.com")
                .passwordHash("hashed-pass")
                .userType("USER")
                .emailVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User updatedInfo = User.builder()
                .id(id)
                .firstName("New")
                .lastName("Name")
                .build();

        when(userRepository.findById(id))
                .thenReturn(Optional.of(existing));

        userService.updateProfile(updatedInfo);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateProfile_UserNotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        User update = User.builder().id(id).build();

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile(update));
    }
}
