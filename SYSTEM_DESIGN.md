# Authentication Service System Design Document

**Version:** 1.0  
**Date:** 2026-02-15  
**Project:** JobHub Authentication Service

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Token Blacklist - Quick Summary](#token-blacklist---quick-summary)
3. [System Architecture Overview](#system-architecture-overview)
4. [Core Components](#core-components)
5. [Security Mechanisms](#security-mechanisms)
6. [Authentication Flows](#authentication-flows)
7. [Data Models](#data-models)
8. [Infrastructure](#infrastructure)
9. [Design Patterns Used](#design-patterns-used)

---

## 1. Executive Summary

The JobHub Authentication Service is a robust, secure authentication microservice built with Spring Boot that provides comprehensive user authentication, authorization, and account management capabilities. The service implements industry-standard security practices including JWT-based authentication, Multi-Factor Authentication (MFA), rate limiting, account lockout protection, and token blacklisting for secure logout.

---

## 2. Token Blacklist - Quick Summary

**What does the Token Blacklist do?**

The Token Blacklist is a security mechanism that **invalidates JWT access tokens when a user logs out**, ensuring that even if someone has a valid token, they cannot use it after logging out. Here's how it works:

| Feature | Description |
|---------|-------------|
| **Purpose** | Invalidates tokens upon user logout to prevent token reuse |
| **Storage** | Redis cache with TTL matching token expiration |
| **Key Format** | `blacklist:token:{jwt_token}` |
| **Operations** | `blacklistToken()`, `isTokenBlacklisted()`, `removeFromBlacklist()` |
| **Fail-Safe** | Fails open (allows token) if Redis is unavailable for availability |

When a user logs out, their access token is added to the blacklist in Redis with a time-to-live (TTL) equal to the token's remaining validity. Every time a request comes in, [`JwtUtils.validateToken()`](src/main/java/com/example/Authentication_System/Security/JwtUtils.java:65) checks the blacklist before accepting the token.

---

## 3. System Architecture Overview

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT APPLICATIONS                               │
│  (Web App, Mobile App, API Gateway)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP Requests (JWT Bearer Token)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY FILTER CHAIN                                │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌────────────────┐  │
│  │ RateLimitingFilter │───▶│ JwtAuthentication   │───▶│ SecurityConfig │  │
│  │                     │    │ Filter              │    │                │  │
│  └─────────────────────┘    └─────────────────────┘    └────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         REST API LAYER                                       │
│  ┌─────────────────────┐    ┌─────────────────────┐                       │
│  │   AuthController     │    │ ProfileController   │                       │
│  │ /api/v1/auth/*       │    │ /api/v1/profile/*   │                       │
│  └─────────────────────┘    └─────────────────────┘                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER                                        │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐   │
│  │ UserServiceImpl   │ │ MfaService       │ │ TokenBlacklistService    │   │
│  │                  │ │                  │ │                          │   │
│  │ - register()     │ │ - setupMfa()      │ │ - blacklistToken()       │   │
│  │ - login()        │ │ - verifyMfa()     │ │ - isTokenBlacklisted()   │   │
│  │ - logout()       │ │ - enableMfa()     │ │ - removeFromBlacklist()  │   │
│  │ - refreshToken() │ │                  │ │                          │   │
│  └──────────────────┘ └──────────────────┘ └──────────────────────────┘   │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐   │
│  │ AccountLockout   │ │ RateLimiting     │ │ AuditService             │   │
│  │ Service          │ │ Service          │ │                          │   │
│  │                  │ │                  │ │ - logEvent()             │   │
│  │ - isLocked()     │ │ - resolveBucket()│ │ - getAuditLogs()         │   │
│  │ - recordFailed() │ │ - resolveLogin() │ │                          │   │
│  │ - recordSuccess()│ │                  │ │                          │   │
│  └──────────────────┘ └──────────────────┘ └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DOMAIN & REPOSITORY LAYER                            │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐   │
│  │ UserRepository   │ │ RefreshTokenRepo │ │ OutboxEventRepository    │   │
│  │                  │ │                  │ │                          │   │
│  │ - findByEmail()  │ │ - save()         │ │ - save()                 │   │
│  │ - findById()     │ │ - revokeToken()  │ │ - findUnprocessed()      │   │
│  │ - save()         │ │ - revokeAll()     │ │ - markProcessed()        │   │
│  └──────────────────┘ └──────────────────┘ └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │ PostgreSQL   │  │   Redis     │  │    Kafka    │  │    Zipkin      │   │
│  │             │  │             │  │             │  │  (Tracing)     │   │
│  │ - Users     │  │ - Blacklist │  │ - Events    │  │                 │   │
│  │ - Sessions  │  │ - Rate Limit│  │ - Profile  │  │                 │   │
│  │ - Audit Logs│  │             │  │   Updates  │  │                 │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Core Components

### 4.1 Controllers

#### AuthController
**Location:** [`src/main/java/com/example/Authentication_System/Controllers/AuthController.java`](src/main/java/com/example/Authentication_System/Controllers/AuthController.java)

The main REST controller handling authentication endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | User registration |
| `/api/v1/auth/login` | POST | User login |
| `/api/v1/auth/refresh` | POST | Refresh access token |
| `/api/v1/auth/logout` | POST | User logout (blacklists token) |
| `/api/v1/auth/profile` | GET/PUT | Get/Update user profile |
| `/api/v1/auth/verify-email` | GET/POST | Email verification |
| `/api/v1/auth/mfa/setup` | POST | Setup MFA |
| `/api/v1/auth/mfa/enable` | POST | Enable MFA |
| `/api/v1/auth/login/mfa` | POST | Verify MFA code |
| `/api/v1/auth/forgot-password` | POST | Password reset request |
| `/api/v1/auth/reset-password` | POST | Password reset |

### 4.2 Services

#### TokenBlacklistService
**Location:** [`src/main/java/com/example/Authentication_System/Services/TokenBlacklistService.java`](src/main/java/com/example/Authentication_System/Services/TokenBlacklistService.java)

The Token Blacklist service manages JWT token invalidation:

```java
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    
    // Add token to blacklist with TTL
    public void blacklistToken(String token, long expiryTimeMillis)
    
    // Check if token is blacklisted
    public boolean isTokenBlacklisted(String token)
    
    // Remove from blacklist (admin/testing)
    public void removeFromBlacklist(String token)
}
```

**Key Design Decisions:**
- Uses Redis for high-performance lookups
- TTL automatically expires blacklisted tokens
- Fails open (allows token) if Redis is unavailable for availability
- Logs errors but continues operation if blacklisting fails

#### MfaService
**Location:** [`src/main/java/com/example/Authentication_System/Services/MfaService.java`](src/main/java/com/example/Authentication_System/Services/MfaService.java)

Implements Time-based One-Time Password (TOTP) for Multi-Factor Authentication:
- Generates secret keys for QR code provisioning
- Validates 6-digit TOTP codes
- Supports enable/disable MFA per user

#### RateLimitingService
**Location:** [`src/main/java/com/example/Authentication_System/Services/RateLimitingService.java`](src/main/java/com/example/Authentication_System/Services/RateLimitingService.java)

Uses Bucket4j library for token bucket rate limiting:

| Bucket Type | Limit |
|-------------|-------|
| General | 10 requests/minute |
| Login (per IP) | 5 attempts/minute |
| Registration (per IP) | 100 attempts/minute |
| User Login | 3 attempts/5 minutes |
| Password Reset | 2 attempts/hour |

#### AccountLockoutService
**Location:** [`src/main/java/com/example/Authentication_System/Services/AccountLockoutService.java`](src/main/java/com/example/Authentication_System/Services/AccountLockoutService.java)

Protects against brute force attacks:
- Locks account after 5 failed login attempts (configurable)
- Progressive lockout duration: 15min (5-6 attempts), 30min (7-9), 60min (10+)
- Progressive delays: exponential backoff (1s, 2s, 4s, 8s...)
- Auto-unlock when lockout period expires

#### AuditService
**Location:** [`src/main/java/com/example/Authentication_System/Services/AuditService.java`](src/main/java/com/example/Authentication_System/Services/AuditService.java)

Comprehensive audit logging for:
- Login successes/failures
- Profile updates
- Password changes
- Account deactivations
- Token operations

---

## 5. Security Mechanisms

### 5.1 JWT Token Management

#### JwtUtils
**Location:** [`src/main/java/com/example/Authentication_System/Security/JwtUtils.java`](src/main/java/com/example/Authentication_System/Security/JwtUtils.java)

```java
@Component
public class JwtUtils {
    // Token Generation
    public String generateAccessToken(User user)  // 15 minutes expiry
    public String generateRefreshToken(User user) // 7 days expiry
    
    // Token Validation
    public boolean validateToken(String token) {
        // 1. Check blacklist first
        // 2. Verify signature with RSA public key
        // 3. Check expiration
    }
    
    // Claims extraction
    public String getUsernameFromToken(String token)
    public String getUserIdFromToken(String token)
    public String getUserTypeFromToken(String token)
}
```

**Token Configuration:**
| Token Type | Expiration | Storage |
|------------|-------------|---------|
| Access Token | 15 minutes (900s) | Client (memory/localStorage) |
| Refresh Token | 7 days | Database (SHA-256 hashed) |

### 5.2 Security Filter Chain

**Location:** [`src/main/java/com/example/Authentication_System/Security/SecurityConfig.java`](src/main/java/com/example/Authentication_System/Security/SecurityConfig.java)

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", ...).permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
}
```

**Filter Order:**
1. RateLimitingFilter - API rate limiting
2. JwtAuthenticationFilter - JWT token validation
3. Spring Security - Authorization

### 5.3 Password Security

- **Algorithm:** BCrypt with cost factor 10+
- **Storage:** Only hash stored, never plaintext
- **Validation:** Matches against stored hash

### 5.4 IP Address Handling

The system extracts client IP from:
1. `X-Forwarded-For` header (for proxied requests)
2. `X-Real-IP` header
3. `request.getRemoteAddr()` (fallback)

---

## 6. Authentication Flows

### 6.1 Login Flow

```
┌──────────┐                    ┌──────────────────┐                    ┌─────────────┐
│  Client  │                    │  Auth Controller │                    │  Services   │
└──────────┘                    └──────────────────┘                    └─────────────┘
      │                                │                                     │
      │  POST /api/v1/auth/login       │                                     │
      │────────────────────────────────▶│                                     │
      │                                │                                     │
      │                                │  login(email, password, IP, UA)    │
      │                                │────────────────────────────────────▶│
      │                                │                                     │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 1. Check account    │    │
      │                                │         │    locked?          │    │
      │                                │         └─────────────────────┘    │
      │                                │                     │              │
      │                                │                     ▼              │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 2. Verify email    │    │
      │                                │         │    verified        │    │
      │                                │         └─────────────────────┘    │
      │                                │                     │              │
      │                                │                     ▼              │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 3. Check password  │    │
      │                                │         │    (BCrypt match)   │    │
      │                                │         └─────────────────────┘    │
      │                                │                     │              │
      │                                │                     ▼              │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 4. Generate JWT    │    │
      │                                │         │    tokens          │    │
      │                                │         └─────────────────────┘    │
      │                                │                                     │
      │               AuthResponse     │                                     │
      │◀────────────────────────────────│                                     │
      │  (accessToken, refreshToken)   │                                     │
```

### 6.2 Logout & Token Blacklist Flow

```
┌──────────┐                    ┌──────────────────┐                    ┌─────────────┐
│  Client  │                    │  Auth Controller │                    │  Services   │
└──────────┘                    └──────────────────┘                    └─────────────┘
      │                                │                                     │
      │  POST /api/v1/auth/logout     │                                     │
      │  (Bearer: <access_token>)      │                                     │
      │────────────────────────────────▶│                                     │
      │                                │                                     │
      │                                │  logout(userId, accessToken, IP, UA)│
      │                                │────────────────────────────────────▶│
      │                                │                                     │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 1. Blacklist JWT   │    │
      │                                │         │    in Redis         │    │
      │                                │         │    (TTL = 15 min)   │    │
      │                                │         └─────────────────────┘    │
      │                                │                     │              │
      │                                │                     ▼              │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 2. Revoke all       │    │
      │                                │         │    refresh tokens   │    │
      │                                │         └─────────────────────┘    │
      │                                │                     │              │
      │                                │                     ▼              │
      │                                │         ┌─────────────────────┐    │
      │                                │         │ 3. Audit log        │    │
      │                                │         │    LOGOUT event     │    │
      │                                │         └─────────────────────┘    │
      │                                │                                     │
      │           200 OK              │                                     │
      │◀────────────────────────────────│                                     │
```

### 6.3 Token Validation Flow (Per Request)

```
┌──────────┐                    ┌──────────────────┐                    ┌─────────────┐
│  Client  │                    │ JwtAuthentication│                    │  Redis      │
│          │                    │     Filter        │                    │  (Blacklist)│
└──────────┘                    └──────────────────┘                    └─────────────┘
      │                                │                                     │
      │  GET /api/v1/auth/profile     │                                     │
      │  (Bearer: <access_token>)    │                                     │
      │────────────────────────────────▶│                                     │
      │                                │                                     │
      │                                │  1. Check blacklist                │
      │                                │────────────────────────────────────▶│
      │                                │         ┌─────────────────────┐    │
      │                                │         │ isTokenBlacklisted()│    │
      │                                │         │ "blacklist:token:.."│    │
      │                                │         └─────────────────────┘    │
      │                                │◀──────────────────────────────────│
      │                                │                                     │
      │                                │  if BLACKLISTED → Reject           │
      │                                │  if NOT → Validate JWT signature  │
      │                                │         and expiration            │
      │                                │                                     │
      │                    ┌──────────┴──────────┐                         │
      │                    │ 2. Set SecurityContext │                       │
      │                    │    with UserDetails    │                       │
      │                    └───────────────────────┘                         │
      │                                │                                     │
      │           200 OK              │                                     │
      │◀────────────────────────────────│                                     │
```

---

## 7. Data Models

### 7.1 Core Entities

| Entity | Description |
|--------|-------------|
| **User** | Core user account with email, password hash, status |
| **UserProfile** | Extended profile (bio, location, phone, avatar) |
| **Role** | User roles (job_seeker, employer, admin) |
| **Permission** | Granular permissions |
| **Session** | Active user sessions |
| **RefreshToken** | Stored refresh tokens (hashed) |
| **AuditLog** | Security audit trail |
| **OutboxEvent** | Event store for Kafka publishing |

### 7.2 Database Schema (Flyway Migrations)

| Migration | Description |
|-----------|-------------|
| V1 | Create users table |
| V2 | Create roles table |
| V3 | Add email/password reset tokens |
| V4 | Create permissions table |
| V5 | Create sessions table |
| V6 | Create user_roles junction table |
| V7 | Create role_permissions junction table |
| V8 | Create audit_logs table |
| V9 | Create refresh_tokens table |
| V10 | Insert default roles |
| V11 | Add account lockout fields |
| V12 | Add activity-based role fields |
| V13 | Create outbox_events table |
| V14 | Create skills table |
| V15 | Create experiences table |
| V16 | Create educations table |

---

## 8. Infrastructure

### 8.1 Docker Compose Services

**Location:** [`docker-compose.yml`](docker-compose.yml)

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Primary database (external) |
| Redis | 6379 | Cache & token blacklist (external) |
| Kafka | 9092 | Event streaming |
| Kafka UI | 8080 | Kafka management UI |
| Zookeeper | 2181 | Kafka coordination |
| Zipkin | 9411 | Distributed tracing |

### 8.2 External Dependencies

- **PostgreSQL:** User data, audit logs, refresh tokens
- **Redis:** Token blacklist, rate limiting buckets
- **Kafka:** Profile change events, user registration events
- **Zipkin:** Request tracing and debugging

---

## 9. Design Patterns Used

### 9.1 Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────────┐
│                        DRIVING ADAPTERS                         │
│  (Controllers, Filters, Security)                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION CORE                           │
│  ┌─────────────────┐    ┌─────────────────────────────────┐    │
│  │  Use Cases      │    │         Domain                  │    │
│  │ (UserService)   │    │  (Entities, Business Logic)      │    │
│  └─────────────────┘    └─────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────┐        │        ┌─────────────────────┐  │
│  │   Domain        │◀───────┼───────▶│   Output Ports       │  │
│  │   Events        │        │        │ (Repository Interfaces)│ │
│  └─────────────────┘        │        └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DRIVEN ADAPTERS                           │
│  (Persistence Adapters, Email, Kafka, Redis)                    │
└─────────────────────────────────────────────────────────────────┘
```

### 9.2 Other Patterns

| Pattern | Usage |
|---------|-------|
| **Repository Pattern** | Abstract data access via interfaces |
| **Service Layer** | Business logic encapsulation |
| **Event-Driven** | Outbox pattern for reliable Kafka publishing |
| **Builder Pattern** | Entity construction with Lombok |
| **DTO** | API request/response objects |
| **Singleton** | Spring managed beans |
| **Factory** | PasswordEncoder, Bucket creation |

---

## Appendix: Configuration Properties

```properties
# JWT Configuration
jwt.access-token.expiration=900000        # 15 minutes
jwt.refresh-token.expiration=604800000    # 7 days

# Account Lockout
security.account-lockout.attempts-threshold=5
security.account-lockout.lock-duration-minutes=15
security.account-lockout.progressive-delays=true

# Rate Limiting (application code)
general: 10 requests/minute
login: 5 attempts/minute per IP
registration: 100 attempts/minute per IP
```

---

*Document generated for JobHub Authentication Service*
