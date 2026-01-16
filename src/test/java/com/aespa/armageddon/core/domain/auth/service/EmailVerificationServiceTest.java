package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService 테스트")
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private RedisTokenStore tokenStore;

    @Mock
    private MailService mailService;

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
    @DisplayName("이메일 인증 요청 테스트")
    class RequestVerificationTest {

        @Test
        @DisplayName("이메일 인증 요청 성공")
        void requestVerification_Success() {
            // given
            String email = "test@example.com";
            doNothing().when(tokenStore).storeEmailVerificationCode(anyString(), anyString(), any(Duration.class));
            doNothing().when(mailService).sendEmailVerificationCode(anyString(), anyString());

            // when
            emailVerificationService.requestVerification(email);

            // then
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(mailService).sendEmailVerificationCode(emailCaptor.capture(), codeCaptor.capture());

            assertThat(emailCaptor.getValue()).isEqualTo(email);
            assertThat(codeCaptor.getValue()).matches("\\d{6}");

            verify(tokenStore).storeEmailVerificationCode(eq(email), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("이메일 인증 요청 실패 - 이메일이 null")
        void requestVerification_Fail_NullEmail() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.requestVerification(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).storeEmailVerificationCode(anyString(), anyString(), any());
            verify(mailService, never()).sendEmailVerificationCode(anyString(), anyString());
        }

        @Test
        @DisplayName("이메일 인증 요청 실패 - 이메일이 빈 문자열")
        void requestVerification_Fail_EmptyEmail() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.requestVerification("   "))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).storeEmailVerificationCode(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("이메일 인증 확인 테스트")
    class ConfirmVerificationTest {

        @Test
        @DisplayName("이메일 인증 확인 성공")
        void confirmVerification_Success() {
            // given
            String email = "test@example.com";
            String code = "123456";
            String codeHash = sha256(code);

            given(tokenStore.getEmailVerificationCode(email)).willReturn(codeHash);

            // when
            emailVerificationService.confirmVerification(email, code);

            // then
            verify(tokenStore).getEmailVerificationCode(email);
            verify(tokenStore).deleteEmailVerificationCode(email);
            verify(tokenStore).storeEmailVerified(eq(email), any(Duration.class));
        }

        @Test
        @DisplayName("이메일 인증 확인 실패 - 이메일이 null")
        void confirmVerification_Fail_NullEmail() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.confirmVerification(null, "123456"))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).getEmailVerificationCode(anyString());
        }

        @Test
        @DisplayName("이메일 인증 확인 실패 - 코드가 null")
        void confirmVerification_Fail_NullCode() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.confirmVerification("test@example.com", null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).getEmailVerificationCode(anyString());
        }

        @Test
        @DisplayName("이메일 인증 확인 실패 - 코드가 빈 문자열")
        void confirmVerification_Fail_EmptyCode() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.confirmVerification("test@example.com", "   "))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).getEmailVerificationCode(anyString());
        }

        @Test
        @DisplayName("이메일 인증 확인 실패 - 저장된 코드가 없음")
        void confirmVerification_Fail_NoStoredCode() {
            // given
            String email = "test@example.com";
            String code = "123456";

            given(tokenStore.getEmailVerificationCode(email)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> emailVerificationService.confirmVerification(email, code))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_EMAIL_VERIFICATION_CODE);
                    });

            verify(tokenStore, never()).deleteEmailVerificationCode(anyString());
        }

        @Test
        @DisplayName("이메일 인증 확인 실패 - 코드 불일치")
        void confirmVerification_Fail_CodeMismatch() {
            // given
            String email = "test@example.com";
            String correctCode = "123456";
            String wrongCode = "654321";
            String storedHash = sha256(correctCode);

            given(tokenStore.getEmailVerificationCode(email)).willReturn(storedHash);

            // when & then
            assertThatThrownBy(() -> emailVerificationService.confirmVerification(email, wrongCode))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_EMAIL_VERIFICATION_CODE);
                    });

            verify(tokenStore, never()).deleteEmailVerificationCode(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 인증 확인 및 소비 테스트")
    class AssertVerifiedAndConsumeTest {

        @Test
        @DisplayName("인증 확인 및 소비 성공")
        void assertVerifiedAndConsume_Success() {
            // given
            String email = "test@example.com";
            given(tokenStore.isEmailVerified(email)).willReturn(true);

            // when
            emailVerificationService.assertVerifiedAndConsume(email);

            // then
            verify(tokenStore).isEmailVerified(email);
            verify(tokenStore).deleteEmailVerified(email);
            verify(tokenStore).deleteEmailVerificationCode(email);
        }

        @Test
        @DisplayName("인증 확인 및 소비 실패 - 이메일이 null")
        void assertVerifiedAndConsume_Fail_NullEmail() {
            // when & then
            assertThatThrownBy(() -> emailVerificationService.assertVerifiedAndConsume(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });

            verify(tokenStore, never()).isEmailVerified(anyString());
        }

        @Test
        @DisplayName("인증 확인 및 소비 실패 - 이메일 인증 미완료")
        void assertVerifiedAndConsume_Fail_NotVerified() {
            // given
            String email = "test@example.com";
            given(tokenStore.isEmailVerified(email)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> emailVerificationService.assertVerifiedAndConsume(email))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.EMAIL_VERIFICATION_REQUIRED);
                    });

            verify(tokenStore, never()).deleteEmailVerified(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 인증 정보 삭제 테스트")
    class DeleteByEmailTest {

        @Test
        @DisplayName("이메일 인증 정보 삭제 성공")
        void deleteByEmail_Success() {
            // given
            String email = "test@example.com";

            // when
            emailVerificationService.deleteByEmail(email);

            // then
            verify(tokenStore).deleteEmailVerificationCode(email);
            verify(tokenStore).deleteEmailVerified(email);
        }

        @Test
        @DisplayName("이메일 인증 정보 삭제 - 이메일이 null인 경우 무시")
        void deleteByEmail_NullEmail() {
            // when
            emailVerificationService.deleteByEmail(null);

            // then
            verify(tokenStore, never()).deleteEmailVerificationCode(anyString());
            verify(tokenStore, never()).deleteEmailVerified(anyString());
        }

        @Test
        @DisplayName("이메일 인증 정보 삭제 - 빈 문자열인 경우 무시")
        void deleteByEmail_EmptyEmail() {
            // when
            emailVerificationService.deleteByEmail("   ");

            // then
            verify(tokenStore, never()).deleteEmailVerificationCode(anyString());
            verify(tokenStore, never()).deleteEmailVerified(anyString());
        }
    }
}
