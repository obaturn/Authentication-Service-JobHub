package com.example.Authentication_System.Domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvatarUploadRequest {

    @NotBlank(message = "Avatar URL is required")
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
}
