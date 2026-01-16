package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.api.auth.dto.request.LoginRequest;
import com.aespa.armageddon.core.api.auth.dto.response.TokenResponse;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenStore tokenStore;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CoreException(ErrorType.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getLoginId());

        tokenStore.storeRefreshToken(
                user.getLoginId(),
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpiration())
        );

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration);
    }

    @Transactional
    public TokenResponse refreshToken(String providedRefreshToken) {
        jwtTokenProvider.validateToken(providedRefreshToken);
        String loginId = jwtTokenProvider.getLoginIdFromJWT(providedRefreshToken);

        String storedToken = tokenStore.getRefreshToken(loginId);
        if (storedToken == null || !storedToken.equals(providedRefreshToken)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getLoginId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getLoginId());

        tokenStore.storeRefreshToken(
                user.getLoginId(),
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshExpiration())
        );

        return TokenResponse.of(accessToken, refreshToken, accessTokenExpiration);
    }

    @Transactional
    public void logout(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        String loginId = jwtTokenProvider.getLoginIdFromJWT(refreshToken);
        tokenStore.deleteRefreshToken(loginId);
    }
}
