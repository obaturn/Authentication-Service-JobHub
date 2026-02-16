package com.example.Authentication_System.Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.Base64;

@Service
@Slf4j
public class MfaService {

    public String generateNewSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    public String generateQrCodeImageUri(String secret, String email, String issuer) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data.getUri(), BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    /**
     * Manual TOTP calculation according to RFC 6238
     */
    private String calculateTotpManual(String secret, long timeWindow) {
        try {
            // Decode Base32 secret
            byte[] secretBytes = decodeBase32(secret);
            
            // Convert timeWindow to 8-byte big-endian
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(timeWindow);
            byte[] timeBytes = buffer.array();
            
            // Calculate HMAC-SHA1
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA1");
            mac.init(keySpec);
            byte[] hmac = mac.doFinal(timeBytes);
            
            // Dynamic truncation
            int offset = hmac[hmac.length - 1] & 0x0F;
            int binary = ((hmac[offset] & 0x7F) << 24) |
                        ((hmac[offset + 1] & 0xFF) << 16) |
                        ((hmac[offset + 2] & 0xFF) << 8) |
                        (hmac[offset + 3] & 0xFF);
            
            // Generate OTP
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            System.out.println("[MFA_DEBUG] Manual TOTP error: " + e.getMessage());
            return "ERROR";
        }
    }
    
    /**
     * Decode Base32 string to bytes
     */
    private byte[] decodeBase32(String base32) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        base32 = base32.toUpperCase().replace("=", "");
        ByteBuffer buffer = ByteBuffer.allocate(base32.length() * 5 / 8);
        int bits = 0;
        int value = 0;
        
        for (char c : base32.toCharArray()) {
            int index = alphabet.indexOf(c);
            if (index < 0) continue;
            value = (value << 5) | index;
            bits += 5;
            
            if (bits >= 8) {
                bits -= 8;
                buffer.put((byte) (value >>> bits));
            }
        }
        return buffer.array();
    }

    public boolean isOtpValid(String secret, String code) {
        System.out.println("[MFA_DEBUG] === TOTP Verification ===");
        System.out.println("[MFA_DEBUG] Secret (length): " + secret.length());
        System.out.println("[MFA_DEBUG] Secret (full): " + secret);
        System.out.println("[MFA_DEBUG] Code provided: " + code);
        
        // Calculate current time in epoch seconds
        LocalDateTime localDateTime = LocalDateTime.now();
        long currentTimeSeconds = localDateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
        
        System.out.println("[MFA_DEBUG] LocalDateTime.now(): " + localDateTime);
        System.out.println("[MFA_DEBUG] Epoch seconds: " + currentTimeSeconds);
        
        // The time window should be epoch/30
        long timeWindow = currentTimeSeconds / 30;
        System.out.println("[MFA_DEBUG] Time window (epoch/30): " + timeWindow);
        
        // Generate codes using manual TOTP for verification
        System.out.println("[MFA_DEBUG] === Manual TOTP Calculation ===");
        
        // Check multiple time windows (±2 windows = 5 total)
        long[] windows = {timeWindow - 2, timeWindow - 1, timeWindow, timeWindow + 1, timeWindow + 2};
        
        for (long window : windows) {
            String manualCode = calculateTotpManual(secret, window);
            System.out.println("[MFA_DEBUG] Manual Window " + window + " (code=" + manualCode + ")");
            
            if (manualCode.equals(code)) {
                System.out.println("[MFA_DEBUG] ✓ MANUAL MATCH FOUND: " + manualCode);
                return true;
            }
        }
        
        System.out.println("[MFA_DEBUG] ✗ NO MATCH FOUND");
        return false;
    }
}