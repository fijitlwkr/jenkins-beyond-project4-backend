package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.api.auth.dto.request.SignupRequest;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RedisTokenStore tokenStore;

    @Transactional
    public Long signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new CoreException(ErrorType.DUPLICATE_LOGIN_ID);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CoreException(ErrorType.DUPLICATE_EMAIL);
        }

        emailVerificationService.assertVerifiedAndConsume(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .loginId(request.getLoginId())
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }

    @Transactional
    public User updateProfile(String currentLoginId, String currentPassword,
                              String newLoginId, String newEmail, String newNickname) {
        if (currentLoginId == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        String trimmedLoginId = trimToNull(newLoginId);
        String trimmedEmail = trimToNull(newEmail);
        String trimmedNickname = trimToNull(newNickname);
        String trimmedCurrentPassword = trimToNull(currentPassword);

        if (trimmedLoginId == null && trimmedEmail == null && trimmedNickname == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        if (trimmedCurrentPassword == null || !passwordEncoder.matches(trimmedCurrentPassword, user.getPassword())) {
            throw new CoreException(ErrorType.INVALID_PASSWORD);
        }

        boolean loginIdChanged = false;

        if (trimmedLoginId != null && !trimmedLoginId.equals(user.getLoginId())) {
            if (userRepository.existsByLoginId(trimmedLoginId)) {
                throw new CoreException(ErrorType.DUPLICATE_LOGIN_ID);
            }
            user.updateLoginId(trimmedLoginId);
            loginIdChanged = true;
        }

        if (trimmedEmail != null && !trimmedEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(trimmedEmail)) {
                throw new CoreException(ErrorType.DUPLICATE_EMAIL);
            }
            emailVerificationService.assertVerifiedAndConsume(trimmedEmail);
            user.updateEmail(trimmedEmail);
        }

        if (trimmedNickname != null && !trimmedNickname.equals(user.getNickname())) {
            user.updateNickname(trimmedNickname);
        }

        if (loginIdChanged) {
            tokenStore.deleteRefreshToken(currentLoginId);
        }

        return user;
    }

    @Transactional
    public void deleteAccount(String currentLoginId) {
        if (currentLoginId == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        tokenStore.deleteRefreshToken(user.getLoginId());
        tokenStore.deletePasswordResetCode(user.getId());
        emailVerificationService.deleteByEmail(user.getEmail());
        userRepository.delete(user);
    }

    public User getProfile(String currentLoginId) {
        if (currentLoginId == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        return userRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
