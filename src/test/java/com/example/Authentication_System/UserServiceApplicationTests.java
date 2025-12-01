package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import com.example.Authentication_System.Services.UserServiceImplementations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceApplicationTests {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImplementations userService;
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserServiceImplementations(userRepository, passwordEncoder);
    }

	@Test

    void register_Successful() {
        User inputUser = User.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-pass");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(savedUserCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.register(inputUser);

        assertNotNull(registered.getId());
        assertEquals("hashed-pass", registered.getPassword());
        assertEquals("John Doe", registered.getFullName());
        assertEquals("john@example.com", registered.getEmail());
        assertEquals("USER", registered.getRole());
        assertTrue(registered.isActive());
        assertNotNull(registered.getCreatedAt());
        assertNotNull(registered.getUpdatedAt());

        verify(userRepository).save(any(User.class));
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
                .password("hashed-pass")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("password123", "hashed-pass"))
                .thenReturn(true);

        Optional<User> result =
                userService.login("john@example.com", "password123");

        assertTrue(result.isPresent());
    }

    @Test
    void login_WrongPassword_ReturnsEmpty() {
        User existing = User.builder()
                .email("john@example.com")
                .password("hashed-pass")
                .build();

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(existing));

        when(passwordEncoder.matches("wrong", "hashed-pass"))
                .thenReturn(false);

        Optional<User> result =
                userService.login("john@example.com", "wrong");

        assertTrue(result.isEmpty());
    }

    @Test
    void login_UserNotFound_ReturnsEmpty() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        Optional<User> result =
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
                .fullName("Old Name")
                .email("john@example.com")
                .password("hashed-pass")
                .role("USER")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User updatedInfo = User.builder()
                .id(id)
                .fullName("New Name")
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
