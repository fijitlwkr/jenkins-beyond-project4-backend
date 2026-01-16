package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.api.auth.dto.request.LoginRequest;
import com.aespa.armageddon.core.api.auth.dto.response.TokenResponse;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTokenStore tokenStore;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .loginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600000L);
    }

    private LoginRequest createLoginRequest(String loginId, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() {
            // given
            LoginRequest request = createLoginRequest("testuser", "password123");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            given(userRepository.findByLoginId(request.getLoginId())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).willReturn(true);
            given(jwtTokenProvider.createToken(testUser.getId(), testUser.getLoginId())).willReturn(accessToken);
            given(jwtTokenProvider.createRefreshToken(testUser.getId(), testUser.getLoginId())).willReturn(refreshToken);
            given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

            // when
            TokenResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(accessToken);
            assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
            verify(tokenStore).storeRefreshToken(eq(testUser.getLoginId()), eq(refreshToken), any());
        }

        @Test
        @DisplayName("로그인 실패 - 사용자를 찾을 수 없음")
        void login_Fail_UserNotFound() {
            // given
            LoginRequest request = createLoginRequest("nonexistent", "password123");
            given(userRepository.findByLoginId(request.getLoginId())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_CREDENTIALS);
                    });

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void login_Fail_InvalidPassword() {
            // given
            LoginRequest request = createLoginRequest("testuser", "wrongpassword");
            given(userRepository.findByLoginId(request.getLoginId())).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_CREDENTIALS);
                    });

            verify(jwtTokenProvider, never()).createToken(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("토큰 갱신 성공")
        void refreshToken_Success() {
            // given
            String providedRefreshToken = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            given(jwtTokenProvider.validateToken(providedRefreshToken)).willReturn(true);
            given(jwtTokenProvider.getLoginIdFromJWT(providedRefreshToken)).willReturn("testuser");
            given(tokenStore.getRefreshToken("testuser")).willReturn(providedRefreshToken);
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(jwtTokenProvider.createToken(testUser.getId(), testUser.getLoginId())).willReturn(newAccessToken);
            given(jwtTokenProvider.createRefreshToken(testUser.getId(), testUser.getLoginId())).willReturn(newRefreshToken);
            given(jwtTokenProvider.getRefreshExpiration()).willReturn(604800000L);

            // when
            TokenResponse response = authService.refreshToken(providedRefreshToken);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
            verify(tokenStore).storeRefreshToken(eq(testUser.getLoginId()), eq(newRefreshToken), any());
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
        void refreshToken_Fail_InvalidToken() {
            // given
            String invalidToken = "invalid-token";
            doThrow(new CoreException(ErrorType.UNAUTHORIZED))
                    .when(jwtTokenProvider).validateToken(invalidToken);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 만료된 토큰")
        void refreshToken_Fail_ExpiredToken() {
            // given
            String expiredToken = "expired-token";
            doThrow(new CoreException(ErrorType.SESSION_EXPIRED))
                    .when(jwtTokenProvider).validateToken(expiredToken);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(expiredToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.SESSION_EXPIRED);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 저장된 토큰과 불일치")
        void refreshToken_Fail_TokenMismatch() {
            // given
            String providedRefreshToken = "provided-token";
            String storedRefreshToken = "different-stored-token";

            given(jwtTokenProvider.validateToken(providedRefreshToken)).willReturn(true);
            given(jwtTokenProvider.getLoginIdFromJWT(providedRefreshToken)).willReturn("testuser");
            given(tokenStore.getRefreshToken("testuser")).willReturn(storedRefreshToken);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(providedRefreshToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 저장된 토큰이 없음")
        void refreshToken_Fail_NoStoredToken() {
            // given
            String providedRefreshToken = "provided-token";

            given(jwtTokenProvider.validateToken(providedRefreshToken)).willReturn(true);
            given(jwtTokenProvider.getLoginIdFromJWT(providedRefreshToken)).willReturn("testuser");
            given(tokenStore.getRefreshToken("testuser")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(providedRefreshToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
        void refreshToken_Fail_UserNotFound() {
            // given
            String providedRefreshToken = "valid-refresh-token";

            given(jwtTokenProvider.validateToken(providedRefreshToken)).willReturn(true);
            given(jwtTokenProvider.getLoginIdFromJWT(providedRefreshToken)).willReturn("testuser");
            given(tokenStore.getRefreshToken("testuser")).willReturn(providedRefreshToken);
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(providedRefreshToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() {
            // given
            String refreshToken = "valid-refresh-token";
            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getLoginIdFromJWT(refreshToken)).willReturn("testuser");

            // when
            authService.logout(refreshToken);

            // then
            verify(jwtTokenProvider).validateToken(refreshToken);
            verify(jwtTokenProvider).getLoginIdFromJWT(refreshToken);
            verify(tokenStore).deleteRefreshToken("testuser");
        }

        @Test
        @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
        void logout_Fail_InvalidToken() {
            // given
            String invalidToken = "invalid-token";
            doThrow(new CoreException(ErrorType.UNAUTHORIZED))
                    .when(jwtTokenProvider).validateToken(invalidToken);

            // when & then
            assertThatThrownBy(() -> authService.logout(invalidToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
                    });

            verify(tokenStore, never()).deleteRefreshToken(anyString());
        }

        @Test
        @DisplayName("로그아웃 실패 - 만료된 토큰")
        void logout_Fail_ExpiredToken() {
            // given
            String expiredToken = "expired-token";
            doThrow(new CoreException(ErrorType.SESSION_EXPIRED))
                    .when(jwtTokenProvider).validateToken(expiredToken);

            // when & then
            assertThatThrownBy(() -> authService.logout(expiredToken))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.SESSION_EXPIRED);
                    });

            verify(tokenStore, never()).deleteRefreshToken(anyString());
        }
    }
}
