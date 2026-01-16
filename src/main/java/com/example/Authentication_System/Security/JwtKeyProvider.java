package com.example.Authentication_System.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class JwtKeyProvider {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtKeyProvider(@Value("${jwt.keystore.path}") String keystorePath,
                          @Value("${jwt.keystore.password}") String keystorePassword,
                          @Value("${jwt.key.alias}") String keyAlias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(keystorePath);
            keyStore.load(resourceAsStream, keystorePassword.toCharArray());

            this.privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
            this.publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load keystore", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}