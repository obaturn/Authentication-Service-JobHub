package com.example.Authentication_System.Infrastructure.Adapter;

import com.example.Authentication_System.Domain.repository.OutPutRepositoryPort.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceAdapter implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceAdapter.class);

    @Override
    public void sendVerificationEmail(String to, String token) {
        logger.info("Sending verification email to: {}", to);
        logger.info("Verification token: {}", token);
        // In a real implementation, this would call an external email provider (SendGrid, SES, etc.)
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        logger.info("Sending password reset email to: {}", to);
        logger.info("Password reset token: {}", token);
    }
}
