package com.huijulh.study.student;

import com.huijulh.study.common.BusinessException;
import com.huijulh.study.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PhoneProtector {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] secret;
    private final SecretKey aesKey;

    public PhoneProtector(@Value("${study.phone.hash-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        try {
            this.aesKey = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(this.secret), "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot initialize phone encryption", exception);
        }
    }

    public String normalize(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String normalized = phone.trim();
        if (!normalized.matches("^1\\d{10}$")) {
            throw new BusinessException(ErrorCode.INVALID_PHONE, "手机号格式错误");
        }
        return normalized;
    }

    public String hash(String normalizedPhone) {
        if (normalizedPhone == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalizedPhone.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash phone", exception);
        }
    }

    public String encrypt(String normalizedPhone) {
        if (normalizedPhone == null) return null;
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(normalizedPhone.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot encrypt phone", exception);
        }
    }

    public String decrypt(String encryptedPhone) {
        if (encryptedPhone == null || encryptedPhone.isBlank()) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPhone);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot decrypt phone", exception);
        }
    }

    public String mask(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
