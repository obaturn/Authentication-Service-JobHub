# Comprehensive Documentation: JobHub Authentication Service

## Table of Contents
1. [Project Overview](#project-overview)
2. [Software Architecture](#software-architecture)
3. [SOLID Principles Implementation](#solid-principles-implementation)
4. [Tools and Technologies](#tools-and-technologies)
5. [Security Implementation](#security-implementation)
6. [Failure Scenarios and Mitigations](#failure-scenarios-and-mitigations)
7. [System Components](#system-components)
8. [Data Flow](#data-flow)
9. [Deployment and Infrastructure](#deployment-and-infrastructure)

## Project Overview

The JobHub Authentication Service is a comprehensive, secure authentication microservice built with Spring Boot. It provides user registration, login, profile management, email verification, password reset, multi-factor authentication (MFA), and advanced security features like rate limiting, account lockout, and behavior tracking.

The service follows Hexagonal Architecture (Ports and Adapters) pattern, ensuring clean separation of concerns, testability, and maintainability. It implements robust security measures to protect against common attacks like brute force, token theft, and unauthorized access.

## Software Architecture

### Hexagonal Architecture (Ports and Adapters)

The project implements Hexagonal Architecture with three main layers:

#### 1. Domain Layer
- **Location**: `src/main/java/com/example/Authentication_System/Domain/`
- **Purpose**: Contains business logic, domain models, and interfaces (ports)
- **Components**:
  - **Models**: `User`, `AuthResponse`, `RefreshToken`, etc.
  - **Input Ports**: Repository interfaces (`UserRepository`, `RefreshTokenRepository`, etc.)
  - **Output Ports**: Use case interfaces (`UserUseCase`, `EmailService`)

#### 2. Infrastructure Layer
- **Location**: `src/main/java/com/example/Authentication_System/Infrastructure/`
- **Purpose**: External concerns like persistence, messaging, and external services
- **Components**:
  - **Adapters**: Implement domain ports (`UserRepositoryAdapter`, `EmailServiceAdapter`)
  - **Entities**: JPA entities (`UserEntity`, `RefreshTokenEntity`)
  - **Mappers**: Convert between domain and infrastructure models

#### 3. Presentation Layer
- **Location**: `src/main/java/com/example/Authentication_System/Controllers/`
- **Purpose**: REST API endpoints and external interfaces
- **Components**:
  - **Controllers**: `AuthController`, `BehaviourTrackingController`
  - **Security**: `SecurityConfig`, `JwtAuthenticationFilter`

### Architecture Benefits
- **Testability**: Domain logic can be tested independently
- **Technology Independence**: Easy to change databases, frameworks, or external services
- **Maintainability**: Clear separation of concerns
- **Scalability**: Each layer can be scaled independently

## SOLID Principles Implementation

### 1. Single Responsibility Principle (SRP)
- **AuthController**: Handles only HTTP request/response logic
- **UserServiceImplementations**: Manages only user-related business logic
- **JwtUtils**: Responsible solely for JWT token operations
- **RateLimitingFilter**: Handles only rate limiting logic

### 2. Open-Closed Principle (OCP)
- **UserUseCase interface**: Allows extension through new implementations
- **Repository interfaces**: Enable different persistence implementations
- **Security filters**: Chainable for adding new security features

### 3. Liskov Substitution Principle (LSP)
- **Repository adapters**: Any adapter implementing `UserRepository` can replace another
- **Service implementations**: `UserServiceImplementations` can be substituted with other implementations of `UserUseCase`

### 4. Interface Segregation Principle (ISP)
- **Specific repository interfaces**: `UserRepository`, `RefreshTokenRepository` instead of a generic repository
- **Focused use case interfaces**: `UserUseCase` contains only user-related operations

### 5. Dependency Inversion Principle (DIP)
- **Domain depends on abstractions**: Domain layer defines interfaces (ports)
- **Infrastructure implements abstractions**: Adapters implement domain interfaces
- **Dependency injection**: Spring manages dependencies through constructor injection

## Tools and Technologies

### Core Framework
- **Spring Boot 3.5.0**: Modern Java framework for microservices
- **Java 17**: Latest LTS version with enhanced security features
- **Spring Security**: Comprehensive security framework

### Security Tools
- **JJWT (JSON Web Tokens)**: For stateless authentication
- **Bucket4j**: High-performance rate limiting
- **TOTP (Time-based One-Time Password)**: For MFA implementation
- **ZXing**: QR code generation for MFA setup
- **BCrypt**: Password hashing

### Data Persistence
- **Spring Data JPA**: Object-relational mapping
- **PostgreSQL**: Robust relational database
- **Flyway**: Database migration management
- **Redis**: Token blacklisting and distributed caching

### Messaging and Events
- **Spring Kafka**: Event-driven architecture
- **Outbox Pattern**: Reliable event publishing

### Development Tools
- **Lombok**: Reduces boilerplate code
- **ModelMapper**: Object mapping between layers
- **Resilience4j**: Circuit breaker pattern
- **Maven**: Dependency management and build tool

### Infrastructure
- **Docker Compose**: Container orchestration for development
- **Kafka + Zookeeper**: Event streaming platform

## Security Implementation

### Authentication & Authorization

#### JWT with RSA Signing
- **Why chosen**: RSA provides stronger security than HMAC, allows key rotation
- **Implementation**: `JwtUtils` generates tokens signed with RSA-256
- **Key management**: `JwtKeyProvider` handles public/private key pairs
- **Token validation**: Checks signature, expiration, and blacklist status

#### Refresh Token Rotation
- **Why chosen**: Prevents token theft attacks, enables secure token renewal
- **Implementation**: Separate access (15min) and refresh (7 days) tokens
- **Storage**: Hashed refresh tokens in database with salt
- **Rotation**: Old refresh tokens revoked on use

#### Token Blacklisting
- **Why chosen**: Allows immediate logout and compromised token invalidation
- **Implementation**: Redis-based blacklist with TTL matching token expiration
- **Performance**: Fast lookups prevent replay attacks

### Multi-Factor Authentication (MFA)
- **Why chosen**: Adds second layer of security beyond passwords
- **Implementation**: TOTP-based using Google Authenticator standard
- **Setup**: QR code generation for easy app integration
- **Verification**: Time-window validation with clock skew tolerance

### Rate Limiting
- **Why chosen**: Prevents brute force and DoS attacks
- **Implementation**: Bucket4j algorithm with Redis backend
- **Configuration**: Different limits for login (5/min) and registration (3/min)
- **IP-based**: Tracks requests per IP address

### Account Lockout
- **Why chosen**: Progressive delay prevents automated attacks
- **Implementation**: Tracks failed attempts, applies exponential backoff
- **Progressive delay**: 1s, 2s, 4s... up to account lockout
- **Reset**: Successful login clears counter

### Audit Logging
- **Why chosen**: Security monitoring and compliance
- **Implementation**: Logs all authentication events with IP and user agent
- **Storage**: Database persistence for analysis
- **Events**: Login success/failure, profile changes, token operations

### Behavior Tracking
- **Why chosen**: Detects anomalous user behavior
- **Implementation**: Tracks login patterns, device fingerprints
- **Hybrid RBAC**: Role-based + behavior-based access control
- **Profiling**: Machine learning-ready data collection

### Email Verification
- **Why chosen**: Ensures valid email ownership
- **Implementation**: Token-based verification with 48-hour expiration
- **Idempotent**: Multiple verification attempts handled gracefully
- **Event-driven**: Uses outbox pattern for reliable email sending

### Password Security
- **Why chosen**: Protects against credential stuffing
- **Implementation**: BCrypt hashing with salt
- **Validation**: Custom validator ensures strong passwords
- **Reset flow**: Secure token-based password reset

## Failure Scenarios and Mitigations

### 1. Database Connection Failure
- **Scenario**: PostgreSQL becomes unavailable
- **Mitigation**: Connection pooling, retry logic, circuit breaker (Resilience4j)
- **Fallback**: Graceful degradation, error responses

### 2. Redis Failure
- **Scenario**: Token blacklist unavailable
- **Mitigation**: Redis cluster, fallback to in-memory cache
- **Impact**: Temporary inability to blacklist tokens, but validation still works

### 3. Kafka Failure
- **Scenario**: Event publishing fails
- **Mitigation**: Outbox pattern ensures events are stored locally and retried
- **Recovery**: Background job processes pending events

### 4. JWT Key Compromise
- **Scenario**: Private key leaked
- **Mitigation**: Key rotation capability, monitor for anomalies
- **Response**: Immediate key rotation, token invalidation

### 5. Brute Force Attacks
- **Scenario**: Automated login attempts
- **Mitigations**:
  - Rate limiting (Bucket4j)
  - Account lockout with progressive delay
  - CAPTCHA integration ready
  - IP-based blocking

### 6. Token Theft
- **Scenario**: Access token stolen via XSS/MITM
- **Mitigations**:
  - Short token expiration (15 minutes)
  - HTTPS enforcement
  - Token blacklisting on logout
  - Refresh token rotation

### 7. Replay Attacks
- **Scenario**: Captured tokens reused
- **Mitigations**:
  - Token blacklisting
  - One-time use refresh tokens
  - Timestamp validation in JWT

### 8. MFA Bypass
- **Scenario**: Attacker bypasses second factor
- **Mitigations**:
  - Secure TOTP implementation
  - Rate limiting on MFA attempts
  - Device fingerprinting

### 9. Email Spoofing
- **Scenario**: Fake verification emails
- **Mitigations**:
  - Token-based verification (not link-based)
  - Email service authentication
  - Domain verification

### 10. Race Conditions
- **Scenario**: Concurrent requests cause inconsistent state
- **Mitigations**:
  - Database transactions (@Transactional)
  - Optimistic locking
  - Idempotent operations

## System Components

### Controllers
- **AuthController**: Main authentication endpoints
- **BehaviourTrackingController**: User behavior analytics
- **GlobalExceptionHandler**: Centralized error handling

### Services
- **UserServiceImplementations**: Core business logic
- **EmailService**: Email sending operations
- **MfaService**: TOTP operations
- **AuditService**: Security event logging
- **AccountLockoutService**: Failed attempt management
- **TokenBlacklistService**: Token invalidation
- **RateLimitingService**: Request throttling

### Security Components
- **SecurityConfig**: Spring Security configuration
- **JwtAuthenticationFilter**: Token validation filter
- **RateLimitingFilter**: Request rate limiting
- **JwtUtils**: Token generation/validation
- **JwtKeyProvider**: RSA key management

### Infrastructure Components
- **Repository Adapters**: Data access implementations
- **Kafka Event Publisher**: Asynchronous messaging
- **Outbox Event Publisher**: Reliable event publishing

## Data Flow

### User Registration
1. Client sends registration request
2. Rate limiting check
3. Input validation
4. Email uniqueness check
5. Password hashing
6. User creation with verification token
7. Outbox event creation for email
8. Audit log entry
9. Response with user data

### User Login
1. Client sends login credentials
2. Rate limiting check
3. Account lockout check
4. Email verification check
5. Password validation
6. MFA check (if enabled)
7. Token generation (access + refresh)
8. Refresh token storage
9. Audit log entry
10. Response with tokens

### Token Refresh
1. Client sends refresh token
2. Token validation and matching
3. Expiration check
4. New token generation
5. Old token revocation
6. Audit log entry
7. Response with new tokens

## Deployment and Infrastructure

### Development Environment
- **Docker Compose**: Kafka, Zookeeper, Kafka UI
- **PostgreSQL**: Local database instance
- **Redis**: Local cache instance
- **Spring Profiles**: Development configuration

### Production Considerations
- **Database**: PostgreSQL cluster with replication
- **Cache**: Redis cluster for high availability
- **Message Queue**: Kafka cluster with partitioning
- **Load Balancing**: API Gateway for request distribution
- **Monitoring**: Application metrics and health checks
- **Security**: Network segmentation, encrypted communications

### Configuration Management
- **Environment Variables**: Sensitive data (keys, DB credentials)
- **Spring Profiles**: Environment-specific configurations
- **External Config**: Centralized configuration service

### Scalability Features
- **Stateless Design**: Horizontal scaling possible
- **Database Sharding**: Ready for user data partitioning
- **Caching Strategy**: Redis for session and blacklist data
- **Event-Driven**: Asynchronous processing for non-blocking operations

This comprehensive authentication service provides enterprise-grade security while maintaining clean architecture and high maintainability. The implementation covers all major security threats and provides robust failure handling mechanisms.