package com.example.Authentication_System;

import com.example.Authentication_System.Domain.model.User;
import com.example.Authentication_System.Domain.model.UserProfile;
import com.example.Authentication_System.Domain.repository.inputRepositoryPort.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class UserServiceApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	@Test
	void contextLoads() {
	}

	@Test
	void testUserCreationAndRetrieval() {
		// Create a user profile
		UserProfile userProfile = UserProfile.builder()
				.bio("This is a test bio.")
				.location("Test City")
				.phone("1234567890")
				.build();

		// Create a user
		User newUser = User.builder()
				.firstName("John")
				.lastName("Doe")
				.email("john.doe.test@example.com")
				.passwordHash(passwordEncoder.encode("password"))
				.userType("job_seeker")
				.status("active")
				.emailVerified(true)
				.mfaEnabled(false)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		// Link the profile to the user
		newUser.setUserProfile(userProfile);

		// Save the user (and profile due to cascade)
		userRepository.save(newUser);
		entityManager.flush(); // Force the SQL INSERT to happen
		entityManager.clear(); // Clear the persistence context to force a fresh read from the DB

		// Retrieve the user
		User foundUser = userRepository.findByEmail("john.doe.test@example.com").orElse(null);

		// Assertions
		assertNotNull(foundUser);
		assertEquals("John", foundUser.getFirstName());
		assertNotNull(foundUser.getUserProfile());
		assertEquals("This is a test bio.", foundUser.getUserProfile().getBio());
	}
}