package com.aespa.armageddon.core.api.auth.controller;

import com.aespa.armageddon.core.api.auth.dto.request.LoginRequest;
import com.aespa.armageddon.core.api.auth.dto.request.RefreshRequest;
import com.aespa.armageddon.core.api.auth.dto.request.SignupRequest;
import com.aespa.armageddon.core.api.auth.dto.response.TokenResponse;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.common.support.error.GlobalExceptionHandler;
import com.aespa.armageddon.core.domain.auth.service.AuthService;
import com.aespa.armageddon.core.domain.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AuthController authController;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SignupRequest createSignupRequest(String loginId, String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private LoginRequest createLoginRequest(String loginId, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "loginId", loginId);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private RefreshRequest createRefreshRequest(String refreshToken) {
        RefreshRequest request = new RefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", refreshToken);
        return request;
    }

    @Nested
    @DisplayName("회원가입 API 테스트")
    class SignupApiTest {

        @Test
        @DisplayName("회원가입 성공")
        void signup_Success() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "test@example.com", "password123", "테스터");
            given(userService.signup(any(SignupRequest.class))).willReturn(1L);

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 로그인 아이디")
        void signup_Fail_DuplicateLoginId() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "test@example.com", "password123", "테스터");
            given(userService.signup(any(SignupRequest.class)))
                    .willThrow(new CoreException(ErrorType.DUPLICATE_LOGIN_ID));

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U003"));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 이메일")
        void signup_Fail_DuplicateEmail() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "test@example.com", "password123", "테스터");
            given(userService.signup(any(SignupRequest.class)))
                    .willThrow(new CoreException(ErrorType.DUPLICATE_EMAIL));

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U002"));
        }

        @Test
        @DisplayName("회원가입 실패 - 이메일 인증 미완료")
        void signup_Fail_EmailVerificationRequired() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "test@example.com", "password123", "테스터");
            given(userService.signup(any(SignupRequest.class)))
                    .willThrow(new CoreException(ErrorType.EMAIL_VERIFICATION_REQUIRED));

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A006"));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (짧은 loginId)")
        void signup_Fail_ValidationError_ShortLoginId() throws Exception {
            // given
            SignupRequest request = createSignupRequest("ab", "test@example.com", "password123", "테스터");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (잘못된 이메일 형식)")
        void signup_Fail_ValidationError_InvalidEmail() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "invalid-email", "password123", "테스터");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }

        @Test
        @DisplayName("회원가입 실패 - 유효성 검증 실패 (짧은 비밀번호)")
        void signup_Fail_ValidationError_ShortPassword() throws Exception {
            // given
            SignupRequest request = createSignupRequest("testuser", "test@example.com", "short", "테스터");

            // when & then
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }
    }

    @Nested
    @DisplayName("로그인 API 테스트")
    class LoginApiTest {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() throws Exception {
            // given
            LoginRequest request = createLoginRequest("testuser", "password123");
            TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 3600000L);
            given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("로그인 실패 - 잘못된 자격 증명")
        void login_Fail_InvalidCredentials() throws Exception {
            // given
            LoginRequest request = createLoginRequest("testuser", "wrongpassword");
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new CoreException(ErrorType.INVALID_CREDENTIALS));

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A003"));
        }

        @Test
        @DisplayName("로그인 실패 - 유효성 검증 실패 (빈 loginId)")
        void login_Fail_ValidationError_EmptyLoginId() throws Exception {
            // given
            LoginRequest request = createLoginRequest("", "password123");

            // when & then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }
    }

    @Nested
    @DisplayName("토큰 갱신 API 테스트")
    class RefreshApiTest {

        @Test
        @DisplayName("토큰 갱신 성공")
        void refresh_Success() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("valid-refresh-token");
            TokenResponse tokenResponse = TokenResponse.of("new-access-token", "new-refresh-token", 3600000L);
            given(authService.refreshToken(anyString())).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 인증되지 않음")
        void refresh_Fail_Unauthorized() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("invalid-token");
            given(authService.refreshToken(anyString()))
                    .willThrow(new CoreException(ErrorType.UNAUTHORIZED));

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A001"));
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 세션 만료")
        void refresh_Fail_SessionExpired() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("expired-token");
            given(authService.refreshToken(anyString()))
                    .willThrow(new CoreException(ErrorType.SESSION_EXPIRED));

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A004"));
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
        void refresh_Fail_UserNotFound() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("valid-token");
            given(authService.refreshToken(anyString()))
                    .willThrow(new CoreException(ErrorType.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U001"));
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 유효성 검증 실패 (빈 refreshToken)")
        void refresh_Fail_ValidationError_EmptyRefreshToken() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("");

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }
    }

    @Nested
    @DisplayName("로그아웃 API 테스트")
    class LogoutApiTest {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("valid-refresh-token");
            doNothing().when(authService).logout(anyString());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));
        }

        @Test
        @DisplayName("로그아웃 실패 - 인증되지 않음")
        void logout_Fail_Unauthorized() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("invalid-token");
            doThrow(new CoreException(ErrorType.UNAUTHORIZED))
                    .when(authService).logout(anyString());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A001"));
        }

        @Test
        @DisplayName("로그아웃 실패 - 세션 만료")
        void logout_Fail_SessionExpired() throws Exception {
            // given
            RefreshRequest request = createRefreshRequest("expired-token");
            doThrow(new CoreException(ErrorType.SESSION_EXPIRED))
                    .when(authService).logout(anyString());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A004"));
        }
    }
}
