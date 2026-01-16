package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTokenStore tokenStore;
    private final MailService mailService;

    @Transactional
    public void requestVerification(String email) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String code = generate6DigitCode();
        String codeHash = sha256(code);

        tokenStore.storeEmailVerificationCode(normalizedEmail, codeHash, TTL);

        mailService.sendEmailVerificationCode(normalizedEmail, code);
    }

    @Transactional
    public void confirmVerification(String email, String code) {
        String normalizedEmail = trimToNull(email);
        String normalizedCode = trimToNull(code);
        if (normalizedEmail == null || normalizedCode == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String storedHash = tokenStore.getEmailVerificationCode(normalizedEmail);
        if (storedHash == null) {
            throw new CoreException(ErrorType.INVALID_EMAIL_VERIFICATION_CODE);
        }

        String inputHash = sha256(normalizedCode);
        if (!storedHash.equals(inputHash)) {
            throw new CoreException(ErrorType.INVALID_EMAIL_VERIFICATION_CODE);
        }

        tokenStore.deleteEmailVerificationCode(normalizedEmail);
        tokenStore.storeEmailVerified(normalizedEmail, TTL);
    }

    @Transactional
    public void assertVerifiedAndConsume(String email) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        if (!tokenStore.isEmailVerified(normalizedEmail)) {
            throw new CoreException(ErrorType.EMAIL_VERIFICATION_REQUIRED);
        }

        tokenStore.deleteEmailVerified(normalizedEmail);
        tokenStore.deleteEmailVerificationCode(normalizedEmail);
    }

    @Transactional
    public void deleteByEmail(String email) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail != null) {
            tokenStore.deleteEmailVerificationCode(normalizedEmail);
            tokenStore.deleteEmailVerified(normalizedEmail);
        }
    }

    private String generate6DigitCode() {
        int n = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", n);
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
