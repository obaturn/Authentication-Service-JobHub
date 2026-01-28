# Backend Integration Guide: Hybrid RBAC + Behavior System

This guide details the backend changes made to support the behavior-based personalization features.

## 1. Overview

We have implemented a "Sidecar" pattern where user behavior is tracked in a separate `BehaviorProfile` linked to the main `User` account. This allows us to keep the core authentication logic clean while enabling rich personalization.

## 2. New Data Structures

### User Object Update
The `User` object returned from login and profile endpoints now includes a `behaviorProfile` field.

```json
{
  "id": "uuid...",
  "email": "user@example.com",
  "userType": "job_seeker",
  "firstName": "John",
  "lastName": "Doe",
  "behaviorProfile": {
    "viewedJobs": 15,
    "appliedJobs": 3,
    "savedJobs": 5,
    "postedJobs": 0,
    "shortlistedCandidates": 0,
    "timeSpentMinutes": 120,
    "lastActiveCategories": ["engineering", "remote"],
    "lastActiveAt": "2023-10-27T10:00:00Z",
    "engagementLevel": "medium" 
  }
}
```

## 3. API Endpoints

### A. Track User Behavior
Call this endpoint whenever a user performs a significant action.

*   **URL:** `/api/v1/behavior/track`
*   **Method:** `POST`
*   **Headers:** `Authorization: Bearer <token>`
*   **Body:**

**Example 1: Job View**
```json
{
  "event": "job_viewed",
  "jobId": "job-123"
}
```

**Example 2: Job Application**
```json
{
  "event": "job_applied",
  "jobId": "job-123"
}
```

**Example 3: Time Spent (Heartbeat)**
```json
{
  "event": "time_spent",
  "duration": 5 
}
```

### B. Get Behavior Profile
Fetch the latest behavior stats for the current user.

*   **URL:** `/api/v1/behavior/profile`
*   **Method:** `GET`
*   **Headers:** `Authorization: Bearer <token>`
*   **Response:** Returns the `BehaviorProfile` object shown above.

## 4. Frontend Implementation Checklist

1.  **Update Types:** Update your `User` interface to include the optional `behaviorProfile`.
2.  **Integrate Tracker:** Use the `BehaviorTracker` utility class (provided in your design) to call the `/api/v1/behavior/track` endpoint.
3.  **Personalize Dashboard:** Use the `engagementLevel` and counters from `user.behaviorProfile` to conditionally render components in `DashboardOverview.tsx`.

## 5. How it Works

1.  **Role-Based Access (RBAC):** The `userType` field (`job_seeker`, `employer`) still determines which dashboard the user sees.
2.  **Behavior-Based Personalization:** The `behaviorProfile` data determines *what content* is shown inside that dashboard.

*   **New User:** `viewedJobs == 0` -> Show Onboarding.
*   **Active User:** `engagementLevel == 'high'` -> Show AI Recommendations.
