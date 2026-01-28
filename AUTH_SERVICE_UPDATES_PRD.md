now accpr# Authentication Service Updates & Flow Documentation

## 1. Updated Architecture: Hexagonal with Event Publishing

The Authentication Service retains its Hexagonal Architecture but adds event publishing for decoupling.

### Changes to Layers
- **Domain:** Unchanged; still handles `User` and auth rules.
- **Application:** Updated `UserServiceImplementations.register()` to publish events after saving.
- **Infrastructure:** Added `KafkaEventPublisher` to send events.
- **Presentation:** Controllers unchanged.

---

## 2. Updated Core Workflows

### A. Registration with Event Publishing
1. **User Submits Data:** Frontend calls `POST /api/v1/auth/register`.
2. **Validation & Saving:** Validates, hashes password, saves user with token.
3. **Event Publishing:** Publishes `UserRegistered` to `user-events` topic.
4. **Response:** Returns user data (no email sent directly).

### B. Verification Flow
1. **Email Received:** User clicks link → Frontend extracts token.
2. **Verification Call:** Frontend calls `GET /api/v1/auth/verify-email?token=...`.
3. **Update User:** Sets `email_verified=true`, `status="active"`.
4. **Login Allowed:** Only if verified.

### C. Communication with Notification Service
- **Via Kafka:** Auth Service produces events; Notification Service consumes.
- **No Direct Calls:** Ensures loose coupling.

---

## 3. Database Schema (Unchanged)
- Retains `users` table with `email_verification_token`, etc.
- No new tables; events are fire-and-forget.

---

## 4. API Endpoint Updates

| Method | Endpoint | Change | Description |
|--------|----------|--------|-------------|
| POST | `/register` | Added event publishing | Creates user and publishes event |
| GET | `/verify-email` | Unchanged | Verifies token, updates user |

---

## 5. Key Classes & Responsibilities

### `KafkaEventPublisher.java`
- **Role:** The Messenger.
- **Function:** Serializes event data to JSON and sends to Kafka topic.

### Updated `UserServiceImplementations.register()`
- **Role:** The Manager (Enhanced).
- **Function:** After saving user, publishes event asynchronously.

---

## 6. Event Schema Examples

### UserRegistered Event
```json
{
  "eventType": "UserRegistered",
  "userId": "uuid-123",
  "email": "user@example.com",
  "verificationToken": "abc123-def456",
  "firstName": "John",
  "timestamp": "2026-01-26T08:57:00Z"
}
```

---

## 7. Security Features (Unchanged)
- Tokens hashed, rotation intact.
- Audit logs for registration.

---

## FAQ: Integration Questions

### Q: How does the Auth Service communicate with Notification Service?
**A:** Via Kafka events. Auth publishes; Notification consumes. No REST calls.

### Q: What if Kafka is down?
**A:** Registration succeeds; email is retried later. Use dead-letter queues for failures.

### Q: Can the Notification Service be scaled?
**A:** Yes, as it's stateless and event-driven.