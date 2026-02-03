# Error Resolution Report: Authentication Service

This document details the resolution of two critical errors encountered during the development of the Authentication Service.

---

## 1. Concurrent Modification Error (Optimistic Locking Failure)

### The Error
```
org.springframework.orm.ObjectOptimisticLockingFailureException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): [com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity#...]
```
And similarly for `AuditLogEntity` and `RefreshTokenEntity`.

### The Root Cause
**Manual ID Generation vs. Hibernate Generation Strategy.**

*   **The Entity Configuration:** The entities (`UserEntity`, `AuditLogEntity`, etc.) were annotated with `@GeneratedValue(generator = "uuid2")`. This tells Hibernate: "Please generate a UUID for me when I save this entity."
*   **The Code:** In `UserServiceImplementations.java`, we were manually generating IDs using `UUID.randomUUID()` and setting them on the object *before* calling `save()`.
    ```java
    User newUser = User.builder()
            .id(UUID.randomUUID()) // <--- The problem
            .build();
    userRepository.save(newUser);
    ```
*   **The Conflict:** When `save()` is called with a non-null ID, Hibernate assumes it is an **existing** entity (detached) and tries to perform an `UPDATE` or `MERGE`. It checks the database, finds no row with that ID, and throws a `StaleObjectStateException` (wrapped as `ObjectOptimisticLockingFailureException`), thinking the row was deleted by another transaction.

### The Solution
We removed the manual ID generation from the service layer.

**Before:**
```java
User newUser = User.builder()
        .id(UUID.randomUUID()) // Manual ID
        .email(...)
        .build();
```

**After:**
```java
User newUser = User.builder()
        // .id(UUID.randomUUID()) // Removed!
        .email(...)
        .build();
```

By leaving the ID as `null`, Hibernate correctly identifies the object as a **new** entity and performs an `INSERT` statement, generating the ID itself.

---

## 2. "Password Cannot Be More Than 72 Bytes" (BCrypt Error)

### The Error
```
java.lang.IllegalArgumentException: password cannot be more than 72 bytes
```
This occurred during the **Login** flow, even though the input password was only 9 characters long.

### The Investigation
1.  **Initial Hypothesis:** The input password was too long or encoded weirdly.
    *   *Debunked:* Logs showed `Raw length: 9`.
2.  **Second Hypothesis:** The stored hash in the database was invalid.
    *   *Debunked:* Logs showed a valid 60-character BCrypt hash.
3.  **Third Hypothesis:** The `BCrypt` implementation was broken or incompatible.
    *   *Debunked:* We successfully generated and matched a test hash in the logs.
4.  **The Breakthrough:** The error was **NOT** happening during password verification. It was happening **after** successful verification, during **Refresh Token generation**.

### The Root Cause
**Hashing a JWT with BCrypt.**

In the `login` method, after verifying the password, we generated a Refresh Token (which is a long JWT string). We then tried to hash this token using the same `passwordEncoder` (BCrypt) before saving it to the database.

```java
RefreshToken.builder()
    .tokenHash(passwordEncoder.encode(refreshTokenValue)) // <--- The problem
    .build();
```

*   **The Limit:** BCrypt has a strict 72-byte limit on the *input* string.
*   **The Input:** A JWT is typically hundreds of characters long (much > 72 bytes).
*   **The Crash:** When `passwordEncoder.encode()` was called on the JWT, BCrypt threw the exception.

### The Solution
We switched the hashing algorithm for Refresh Tokens from BCrypt to **SHA-256**.

1.  **Why SHA-256?** It handles long inputs (like JWTs) without issues and is fast. Since refresh tokens are high-entropy random strings (or signed JWTs), SHA-256 is secure enough for storage.
2.  **Implementation:** We used the existing `hashRefreshToken` method (or `CryptoUtils`) instead of `passwordEncoder`.

**Fixed Code:**
```java
RefreshToken.builder()
    .tokenHash(hashRefreshToken(refreshTokenValue)) // Used SHA-256
    .build();
```

---

## Summary of Best Practices Applied

1.  **Let Hibernate Manage IDs:** Never manually assign IDs for entities configured with `@GeneratedValue`.
2.  **Know Your Algorithms:**
    *   **BCrypt:** Excellent for **Passwords** (short inputs, slow hashing).
    *   **SHA-256:** Good for **Tokens/API Keys** (long inputs, fast hashing).
3.  **Logging is Key:** We solved the "72 bytes" error by logging the exact point of failure. We initially thought it was the password check, but the logs proved the password check *passed*, pointing us to the next step (token generation).
