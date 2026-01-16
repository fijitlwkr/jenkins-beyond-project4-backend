package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetConfirmRequest;
import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetRequest;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService 테스트")
class PasswordResetServiceTest {

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTokenStore tokenStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .loginId("testuser")
                .email("test@example.com")
                .password("encodedOldPassword")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
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

    @Nested
    @DisplayName("비밀번호 재설정 요청 테스트")
    class RequestResetTest {

        @Test
        @DisplayName("비밀번호 재설정 요청 성공")
        void requestReset_Success() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testuser", "test@example.com");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            doNothing().when(tokenStore).storePasswordResetCode(anyLong(), anyString(), any(Duration.class));
            doNothing().when(mailService).sendPasswordResetCode(anyString(), anyString());

            // when
            passwordResetService.requestReset(request);

            // then
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(mailService).sendPasswordResetCode(eq(testUser.getEmail()), codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("\\d{6}");

            verify(tokenStore).storePasswordResetCode(eq(testUser.getId()), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("비밀번호 재설정 요청 실패 - 요청이 null")
        void requestReset_Fail_NullRequest() {
            // when & then
            assertThatThrownBy(() -> passwordResetService.requestReset(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 요청 실패 - loginId가 null")
        void requestReset_Fail_NullLoginId() {
            // given
            PasswordResetRequest request = new PasswordResetRequest(null, "test@example.com");

            // when & then
            assertThatThrownBy(() -> passwordResetService.requestReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 요청 실패 - email이 null")
        void requestReset_Fail_NullEmail() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testuser", null);

            // when & then
            assertThatThrownBy(() -> passwordResetService.requestReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 요청 실패 - 사용자를 찾을 수 없음")
        void requestReset_Fail_UserNotFound() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("nonexistent", "test@example.com");
            given(userRepository.findByLoginId("nonexistent")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> passwordResetService.requestReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
                    });

            verify(mailService, never()).sendPasswordResetCode(anyString(), anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 요청 실패 - 이메일 불일치")
        void requestReset_Fail_EmailMismatch() {
            // given
            PasswordResetRequest request = new PasswordResetRequest("testuser", "different@example.com");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> passwordResetService.requestReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(mailService, never()).sendPasswordResetCode(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 확인 테스트")
    class ConfirmResetTest {

        @Test
        @DisplayName("비밀번호 재설정 확인 성공")
        void confirmReset_Success() {
            // given
            String code = "123456";
            String codeHash = sha256(code);
            String newPassword = "newPassword123";
            String encodedNewPassword = "encodedNewPassword";

            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", code, newPassword);

            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(tokenStore.getPasswordResetCode(testUser.getId())).willReturn(codeHash);
            given(passwordEncoder.matches(newPassword, testUser.getPassword())).willReturn(false);
            given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

            // when
            passwordResetService.confirmReset(request);

            // then
            verify(tokenStore).deletePasswordResetCode(testUser.getId());
            verify(tokenStore).deleteRefreshToken(testUser.getLoginId());
            assertThat(testUser.getPassword()).isEqualTo(encodedNewPassword);
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - 요청이 null")
        void confirmReset_Fail_NullRequest() {
            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - loginId가 null")
        void confirmReset_Fail_NullLoginId() {
            // given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(null, "123456", "newPassword");

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - code가 null")
        void confirmReset_Fail_NullCode() {
            // given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", null, "newPassword");

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - newPassword가 null")
        void confirmReset_Fail_NullNewPassword() {
            // given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", "123456", null);

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(userRepository, never()).findByLoginId(anyString());
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - 사용자를 찾을 수 없음")
        void confirmReset_Fail_UserNotFound() {
            // given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("nonexistent", "123456", "newPassword");
            given(userRepository.findByLoginId("nonexistent")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_PASSWORD_RESET_CODE);
                    });
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - 저장된 코드가 없음")
        void confirmReset_Fail_NoStoredCode() {
            // given
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", "123456", "newPassword");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(tokenStore.getPasswordResetCode(testUser.getId())).willReturn(null);

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_PASSWORD_RESET_CODE);
                    });
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - 코드 불일치")
        void confirmReset_Fail_CodeMismatch() {
            // given
            String correctCode = "123456";
            String wrongCode = "654321";
            String storedHash = sha256(correctCode);

            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", wrongCode, "newPassword");
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(tokenStore.getPasswordResetCode(testUser.getId())).willReturn(storedHash);

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_PASSWORD_RESET_CODE);
                    });
        }

        @Test
        @DisplayName("비밀번호 재설정 확인 실패 - 기존 비밀번호와 동일")
        void confirmReset_Fail_SameAsOldPassword() {
            // given
            String code = "123456";
            String codeHash = sha256(code);
            String samePassword = "samePassword";

            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("testuser", code, samePassword);
            given(userRepository.findByLoginId("testuser")).willReturn(Optional.of(testUser));
            given(tokenStore.getPasswordResetCode(testUser.getId())).willReturn(codeHash);
            given(passwordEncoder.matches(samePassword, testUser.getPassword())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> passwordResetService.confirmReset(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.SAME_AS_OLD_PASSWORD);
                    });

            verify(tokenStore, never()).deletePasswordResetCode(anyLong());
        }
    }
}
