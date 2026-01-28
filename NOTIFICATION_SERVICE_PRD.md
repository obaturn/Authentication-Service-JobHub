# Notification Service - Architecture & API Documentation

## 1. System Architecture: Onion Architecture

This service follows **Onion Architecture** to ensure clean separation of concerns, with dependencies pointing inward. The core business logic is isolated from external frameworks and infrastructure.

### The Layers
1. **Domain (Core):**
   - **Location:** `src/main/java/.../Domain`
   - **Responsibility:** Contains business entities (e.g., `EmailMessage`), value objects, and domain services (e.g., `EmailNotificationService` for email composition logic).
   - **Dependencies:** None. It knows nothing about Kafka or SendGrid.

2. **Application (Use Cases):**
   - **Location:** `src/main/java/.../Application`
   - **Responsibility:** Orchestrates use cases like "Send Verification Email". Implements interfaces from Domain and coordinates with Infrastructure.
   - **Example:** `SendVerificationEmailUseCase` that processes events and calls domain services.

3. **Infrastructure (Adapters):**
   - **Location:** `src/main/java/.../Infrastructure`
   - **Responsibility:** Handles external integrations like Kafka consumers, SendGrid API, and logging. Implements ports defined in Application/Domain.
   - **Example:** `KafkaEventConsumer`, `SendGridEmailProvider`.

4. **Presentation (API/Events):**
   - **Location:** `src/main/java/.../Presentation`
   - **Responsibility:** Exposes REST endpoints if needed (e.g., for manual triggers) and handles event-driven inputs.
   - **Example:** Event listeners for Kafka topics.

---

## 2. Core Workflows

### A. Event-Driven Email Sending
The service is fully asynchronous, triggered by Kafka events.

1. **Event Consumption:**
   - Listens to `user-events` topic for `UserRegistered` events.
   - Parses event payload: `{ "eventType": "UserRegistered", "userId": "uuid", "email": "user@example.com", "verificationToken": "token", "firstName": "John" }`.

2. **Email Composition:**
   - Generates email content: Subject "Verify Your Email", body with link to frontend (e.g., `http://frontend/verify-email?token=token`).
   - Calls SendGrid API to send email.

3. **Error Handling:**
   - Retries failed sends (e.g., API errors) up to 3 times.
   - Logs failures for monitoring; does not block other events.

### B. Extensibility for Other Notifications
- Easily add new event types (e.g., `PasswordResetRequested`) by creating new use cases and consumers.
- No database; all state comes from events.

---

## 3. Key Components

### `SendVerificationEmailUseCase.java`
- **Role:** The Orchestrator.
- **Function:** Receives event data, composes email via domain service, and delegates sending to infrastructure.

### `KafkaEventConsumer.java`
- **Role:** The Listener.
- **Function:** Consumes Kafka messages, deserializes JSON, and triggers use cases.

### `SendGridEmailProvider.java`
- **Role:** The Sender.
- **Function:** Integrates with SendGrid SDK to send emails. Handles API keys and error responses.

---

## 4. Configuration & Dependencies
- **Kafka:** Topic `user-events`, brokers configured via `application.properties`.
- **SendGrid:** API key from environment variables.
- **Database:** MongoDB Atlas for optional logging (e.g., email send history, failures).
- **Dependencies:** `spring-kafka`, SendGrid Java SDK, `spring-boot-starter-data-mongodb` (if using DB).
- **Stateless Core:** Email sending doesn't require DB, but can use it for audits.

---

## 5. API/Event Reference

### Kafka Events Consumed
| Event Type | Payload Example | Action |
|------------|-----------------|--------|
| `UserRegistered` | `{"eventType": "UserRegistered", "email": "user@example.com", "verificationToken": "abc123"}` | Send verification email |

### Email Templates
- **Verification Email:** "Please click the link to verify: http://frontend/verify-email?token={token}"

---

## 6. Security & Best Practices
- **API Keys:** SendGrid key stored securely (not in code).
- **Logging:** Sensitive data (e.g., tokens) not logged.
- **Rate Limiting:** Handled by SendGrid's limits; monitor usage.
- **Testing:** Use embedded Kafka for unit tests.