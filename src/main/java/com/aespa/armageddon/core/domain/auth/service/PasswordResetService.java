package com.aespa.armageddon.core.domain.auth.service;


import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetConfirmRequest;
import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetRequest;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final RedisTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final Duration TTL = Duration.ofMinutes(10);

    @Transactional
    public void requestReset(PasswordResetRequest req) {
        if (req == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String loginId = trimToNull(req.getLoginId());
        String email = trimToNull(req.getEmail());
        if (loginId == null || email == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
        String savedEmail = trimToNull(user.getEmail());
        if (savedEmail == null || !savedEmail.equalsIgnoreCase(email)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String code = generate6DigitCode();
        String codeHash = sha256(code);

        tokenStore.storePasswordResetCode(user.getId(), codeHash, TTL);

        mailService.sendPasswordResetCode(user.getEmail(), code);
    }

    @Transactional
    public void confirmReset(PasswordResetConfirmRequest req) {
        if (req == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        String loginId = trimToNull(req.getLoginId());
        String code = trimToNull(req.getCode());
        String newPassword = trimToNull(req.getNewPassword());

        if (loginId == null || code == null || newPassword == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_PASSWORD_RESET_CODE));

        String storedHash = tokenStore.getPasswordResetCode(user.getId());
        if (storedHash == null) {
            throw new CoreException(ErrorType.INVALID_PASSWORD_RESET_CODE);
        }

        String inputHash = sha256(code);
        if (!storedHash.equals(inputHash)) {
            throw new CoreException(ErrorType.INVALID_PASSWORD_RESET_CODE);
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CoreException(ErrorType.SAME_AS_OLD_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        tokenStore.deletePasswordResetCode(user.getId());
        tokenStore.deleteRefreshToken(user.getLoginId());
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
