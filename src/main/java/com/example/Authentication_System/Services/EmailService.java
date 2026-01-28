package com.example.Authentication_System.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(String to, String subject, String body) {
        // In a real application, this would integrate with an email provider like SendGrid, AWS SES, etc.
        // For this example, we will just log the email to the console.
        logger.info("=================================================");
        logger.info("Sending Email:");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Body: \n{}", body);
        logger.info("=================================================");
    }

    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify Your Email Address";
        // In a real app, the URL would point to your frontend application
        String verificationUrl = "http://localhost:8080/api/v1/auth/verify-email?token=" + token;
        String body = "Please click the link below to verify your email address:\n" + verificationUrl;
        sendEmail(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset Your Password";
        // In a real app, the URL would point to your frontend application
        String resetUrl = "http://localhost:8080/api/auth/reset-password?token=" + token;
        String body = "Please click the link below to reset your password:\n" + resetUrl;
        sendEmail(to, subject, body);
    }
}