package com.example.AI_Projekt.security;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenCipher  {
    private final SecretKeySpec secretKeySpec;
    private final SecureRandom random = new SecureRandom();

    public TokenCipher(@Value("${app.security.encryption-key}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        if (keyBytes.length != 32) {
            throw new TokenCipherException("Invalid encryption key length",null);
        }
        this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(@NonNull String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new TokenCipherException("Encryption failed",e);
        }
    }

    public String decrypt(String data) {
        try {
            String[] parts = data.split(":");
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] decoded = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException |
                 ArrayIndexOutOfBoundsException e) {
            throw new TokenCipherException("Decryption failed",e);
        }
    }
}
