# JobHub Authentication Service - Backend Architecture & API Documentation

## 1. System Architecture: Hexagonal (Ports & Adapters)

This project follows the **Hexagonal Architecture** (also known as Ports and Adapters). This design pattern isolates the core business logic (Domain) from external concerns like databases, APIs, and frameworks.

### The Layers
1.  **Domain (Core):**
    *   **Location:** `src/main/java/.../Domain`
    *   **Responsibility:** Contains the business rules, models (`User`, `BehaviorProfile`), and interfaces (`UserRepository`).
    *   **Dependencies:** None. It knows nothing about Spring Boot or SQL.

2.  **Application (Services):**
    *   **Location:** `src/main/java/.../Services`
    *   **Responsibility:** Orchestrates the flow of data. It implements the use cases (e.g., "Register User", "Track Behavior").
    *   **Example:** `UserServiceImplementations`, `BehaviorTrackingService`.

3.  **Infrastructure (Adapters):**
    *   **Location:** `src/main/java/.../Infrastructure`
    *   **Responsibility:** Implements the interfaces defined in the Domain. This is where the "dirty work" happens (Database queries, Entity mapping).
    *   **Example:** `RefreshTokenRepositoryAdapter`, `UserMapper`.

4.  **Presentation (API):**
    *   **Location:** `src/main/java/.../Presentation`
    *   **Responsibility:** Handles HTTP requests and responses.
    *   **Example:** `AuthController`, `BehaviorTrackingController`.

---

## 2. Core Workflows

### A. Authentication & Security (JWT)
We use a stateless authentication mechanism using **JSON Web Tokens (JWT)**.

1.  **Registration:**
    *   User submits data.
    *   `@ValidPassword` validator checks password strength.
    *   Password is hashed using **BCrypt**.
    *   User is saved with status `active` and default role `job_seeker`.

2.  **Login:**
    *   User submits Email/Password.
    *   System verifies hash.
    *   **Generates Access Token:** Short-lived (15 mins). Used for API access.
    *   **Generates Refresh Token:** Long-lived (7 days). Stored in DB (hashed).

3.  **Token Refresh (Rotation):**
    *   Client sends Refresh Token.
    *   System verifies token exists and is not expired/revoked.
    *   **Security Action:** The old refresh token is **revoked** (deleted/marked invalid).
    *   **New Tokens:** A completely new Access Token AND Refresh Token are issued.
    *   *Why?* If a token is stolen, it can only be used once.

### B. Hybrid RBAC + Behavior System
We combine standard roles with behavioral tracking to personalize the experience.

1.  **RBAC (Role-Based Access Control):**
    *   Determines **Access** (Can I see the Admin Dashboard?).
    *   Managed by `Role` and `UserRole` tables.
    *   Roles: `job_seeker`, `employer`, `admin`.

2.  **Behavior Tracking (The "Sidecar"):**
    *   Determines **Content** (What jobs do I see?).
    *   Managed by `BehaviorProfile` linked 1-to-1 with `User`.
    *   Tracks: `viewedJobs`, `appliedJobs`, `engagementLevel`.

---

## 3. Database Schema (Entities)

### 1. Users Table (`users`)
The core identity table.
*   `id` (UUID): Primary Key.
*   `email`: Unique identifier.
*   `password_hash`: BCrypt hash.
*   `user_type`: 'job_seeker', 'employer', 'admin'.
*   `status`: 'active', 'suspended'.

### 2. Refresh Tokens Table (`refresh_tokens`)
Manages session persistence.
*   `token_hash`: Hashed version of the token (for security).
*   `user_id`: Owner of the token.
*   `expires_at`: When the token dies.
*   `revoked_at`: If present, the token is dead.

### 3. User Behaviors Table (`user_behaviors`)
Stores analytics data.
*   `user_id`: FK to Users.
*   `viewed_jobs`, `applied_jobs`, `saved_jobs`: Counters.
*   `engagement_level`: Calculated in code (Low/Medium/High).

### 4. Audit Logs Table (`audit_logs`)
Compliance and security tracking.
*   `action`: REGISTER, LOGIN_FAILED, PROFILE_UPDATE.
*   `ip_address`, `user_agent`: Metadata.

---

## 4. API Endpoint Reference

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Description | Input |
| :--- | :--- | :--- | :--- |
| POST | `/register` | Create new account | `{email, password, firstName, lastName, userType}` |
| POST | `/login` | Authenticate user | `{email, password}` |
| POST | `/refresh-token` | Get new tokens | `{refreshToken}` |
| POST | `/logout` | Kill session | `{userId}` |
| GET | `/me` | Get current user profile | Header: `Authorization: Bearer <token>` |

### Behavior Tracking (`/api/v1/behavior`)

| Method | Endpoint | Description | Input |
| :--- | :--- | :--- | :--- |
| POST | `/track` | Record an action | `{event: "job_viewed", jobId: "123"}` |
| GET | `/profile` | Get stats & engagement | Header: `Authorization: Bearer <token>` |

---

## 5. Key Classes & Responsibilities

### `UserMapper.java`
*   **Role:** The Translator.
*   **Function:** Converts `UserEntity` (Database format) to `User` (Java Object) and vice versa. It ensures your database structure doesn't leak into your business logic.

### `JwtUtils.java`
*   **Role:** The Keymaster.
*   **Function:** Generates, signs, and validates JWTs. Extracts email/ID from tokens.

### `UserServiceImplementations.java`
*   **Role:** The Manager.
*   **Function:**
    *   `register()`: Orchestrates account creation + default role assignment.
    *   `login()`: Validates credentials + triggers token generation.
    *   `refreshToken()`: Handles the secure rotation logic.

### `BehaviorTrackingService.java`
*   **Role:** The Analyst.
*   **Function:** Receives events, finds the user's behavior profile (or creates one), updates counters, and saves to DB.

### `AuditService.java`
*   **Role:** The Watchdog.
*   **Function:** Asynchronously records every critical action to the `audit_logs` table for security review.

---

## 6. Security Features Summary
1.  **Password Hashing:** BCrypt (Standard).
2.  **Token Hashing:** Refresh tokens are hashed *before* saving to DB. Even if the DB is hacked, attackers can't use the tokens.
3.  **Token Rotation:** Refresh tokens are one-time use.
4.  **Input Validation:** `@ValidPassword`, `@Email`, `@NotBlank` annotations prevent bad data.
5.  **Audit Trails:** All login failures and sensitive actions are logged.
