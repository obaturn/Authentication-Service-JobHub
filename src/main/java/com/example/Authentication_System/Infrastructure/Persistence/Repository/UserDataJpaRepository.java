package com.example.Authentication_System.Infrastructure.Persistence.Repository;


import com.example.Authentication_System.Infrastructure.Persistence.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDataJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
}
