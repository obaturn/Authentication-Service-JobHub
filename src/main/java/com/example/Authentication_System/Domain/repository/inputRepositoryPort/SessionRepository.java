package com.example.Authentication_System.Domain.repository.inputRepositoryPort;

import com.example.Authentication_System.Domain.model.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findById(UUID id);
    Optional<Session> findByTokenHash(String tokenHash);
    List<Session> findByUserId(UUID userId);
    void deleteById(UUID id);
    void deleteByTokenHash(String tokenHash);
}