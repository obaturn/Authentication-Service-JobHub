package com.example.Authentication_System.Domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private String eventType;
    private String userId;
    private String email;
    private String verificationToken;
    private String firstName;
    private Instant timestamp;
}