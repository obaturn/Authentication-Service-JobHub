# Frontend Integration FAQ

This document answers common questions regarding the integration of the JobHub Authentication & Behavior Service.

## 1. Authentication & Tokens

### Q: When the access token expires (15 mins), what should the frontend do?
**A: Option B - Wait for 401, then Refresh.**
Implement an HTTP Interceptor (e.g., in Axios) that:
1.  Catches any `401 Unauthorized` error.
2.  Pauses the failed request.
3.  Calls `POST /api/v1/auth/refresh-token` with the refresh token.
4.  If successful, retries the original request with the new access token.
5.  If refresh fails, redirects the user to the Login page.

### Q: Where should we store the refresh token?
**A: localStorage (for now).**
The backend expects the refresh token in the **JSON Request Body**, so you must have access to it in JavaScript.
*   **Access Token:** Store in memory (variable) or localStorage.
*   **Refresh Token:** Store in localStorage.

### Q: How long is the refresh token valid?
**A: 7 Days.**

---

## 2. Behavior Tracking

### Q: Do we ALWAYS need the Authorization header for tracking?
**A: YES.**
Anonymous tracking is not supported. You must send the `Authorization: Bearer <token>` header with every `POST /api/v1/behavior/track` request.

### Q: What are the supported event types?
Send these exact strings in the `event` field:

1.  `job_viewed` (requires `jobId`)
2.  `job_applied` (requires `jobId`)
3.  `job_saved` (requires `jobId`)
4.  `job_posted` (requires `jobId`)
5.  `time_spent` (requires `duration` in minutes)

### Q: When tracking fails, what should the frontend do?
**A: Fail Silently.**
*   Do **not** show an error message to the user.
*   Do **not** retry the request (it's not critical data).
*   You may log it to the console for debugging (`console.warn`).

---

## 3. Data & Profiles

### Q: Does the backend automatically create a BehaviorProfile on register?
**A: No.**
It is created "lazily" (on demand) the first time you call `/track` or `/profile`.
*   **Default Values:** `viewedJobs: 0`, `engagementLevel: "low"`.

### Q: Is engagementLevel calculated by the backend?
**A: YES.**
It is calculated dynamically every time you fetch the profile. You do not need to calculate it on the frontend.

---

## 4. CORS & Configuration

### Q: Is CORS configured for localhost:5173?
**A: YES.**
The backend is configured to allow requests from `http://localhost:5173`.
*   **Allowed Headers:** `Authorization`, `Content-Type`.
*   **Allowed Methods:** `GET`, `POST`, `PUT`, `DELETE`.

---

## 5. JSON Response Examples

### Login Success
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d87429...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": "uuid...",
    "email": "user@example.com",
    "userType": "job_seeker",
    "behaviorProfile": {
       "viewedJobs": 5,
       "engagementLevel": "low"
    }
  }
}
```

### Behavior Profile
```json
{
  "id": 101,
  "viewedJobs": 12,
  "appliedJobs": 2,
  "savedJobs": 5,
  "postedJobs": 0,
  "shortlistedCandidates": 0,
  "timeSpentMinutes": 45,
  "lastActiveCategories": [],
  "lastActiveAt": "2023-10-27T14:30:00Z",
  "engagementLevel": "medium"
}
```
