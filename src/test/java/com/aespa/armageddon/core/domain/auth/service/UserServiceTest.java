package com.aespa.armageddon.core.domain.auth.service;

import com.aespa.armageddon.core.api.auth.dto.request.SignupRequest;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

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
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {

        private SignupRequest createSignupRequest() {
            SignupRequest request = new SignupRequest();
            ReflectionTestUtils.setField(request, "loginId", "newuser");
            ReflectionTestUtils.setField(request, "email", "new@example.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            ReflectionTestUtils.setField(request, "nickname", "신규유저");
            return request;
        }

        @Test
        @DisplayName("회원가입 성공")
        void signup_Success() {
            // given
            SignupRequest request = createSignupRequest();
            User savedUser = User.builder()
                    .loginId(request.getLoginId())
                    .email(request.getEmail())
                    .password("encodedPassword")
                    .nickname(request.getNickname())
                    .build();
            ReflectionTestUtils.setField(savedUser, "id", 1L);

            given(userRepository.existsByLoginId(request.getLoginId())).willReturn(false);
            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            doNothing().when(emailVerificationService).assertVerifiedAndConsume(request.getEmail());
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            Long userId = userService.signup(request);

            // then
            assertThat(userId).isEqualTo(1L);
            verify(userRepository).existsByLoginId(request.getLoginId());
            verify(userRepository).existsByEmail(request.getEmail());
            verify(emailVerificationService).assertVerifiedAndConsume(request.getEmail());
            verify(passwordEncoder).encode(request.getPassword());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 로그인 아이디")
        void signup_Fail_DuplicateLoginId() {
            // given
            SignupRequest request = createSignupRequest();
            given(userRepository.existsByLoginId(request.getLoginId())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID);
                    });

            verify(userRepository).existsByLoginId(request.getLoginId());
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복된 이메일")
        void signup_Fail_DuplicateEmail() {
            // given
            SignupRequest request = createSignupRequest();
            given(userRepository.existsByLoginId(request.getLoginId())).willReturn(false);
            given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.DUPLICATE_EMAIL);
                    });

            verify(userRepository).existsByLoginId(request.getLoginId());
            verify(userRepository).existsByEmail(request.getEmail());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 이메일 인증 미완료")
        void signup_Fail_EmailVerificationRequired() {
            // given
            SignupRequest request = createSignupRequest();
            given(userRepository.existsByLoginId(request.getLoginId())).willReturn(false);
            given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
            doThrow(new CoreException(ErrorType.EMAIL_VERIFICATION_REQUIRED))
                    .when(emailVerificationService).assertVerifiedAndConsume(request.getEmail());

            // when & then
            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.EMAIL_VERIFICATION_REQUIRED);
                    });

            verify(emailVerificationService).assertVerifiedAndConsume(request.getEmail());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("프로필 수정 테스트")
    class UpdateProfileTest {

        @Test
        @DisplayName("프로필 수정 성공 - 닉네임 변경")
        void updateProfile_Success_NicknameChange() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newNickname = "새닉네임";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);

            // when
            User updated = userService.updateProfile(currentLoginId, currentPassword, null, null, newNickname);

            // then
            assertThat(updated.getNickname()).isEqualTo(newNickname);
            verify(userRepository).findByLoginId(currentLoginId);
            verify(passwordEncoder).matches(currentPassword, testUser.getPassword());
        }

        @Test
        @DisplayName("프로필 수정 성공 - 로그인 아이디 변경")
        void updateProfile_Success_LoginIdChange() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newLoginId = "newloginid";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(userRepository.existsByLoginId(newLoginId)).willReturn(false);

            // when
            User updated = userService.updateProfile(currentLoginId, currentPassword, newLoginId, null, null);

            // then
            assertThat(updated.getLoginId()).isEqualTo(newLoginId);
            verify(tokenStore).deleteRefreshToken(currentLoginId);
        }

        @Test
        @DisplayName("프로필 수정 성공 - 이메일 변경")
        void updateProfile_Success_EmailChange() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newEmail = "newemail@example.com";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(userRepository.existsByEmail(newEmail)).willReturn(false);
            doNothing().when(emailVerificationService).assertVerifiedAndConsume(newEmail);

            // when
            User updated = userService.updateProfile(currentLoginId, currentPassword, null, newEmail, null);

            // then
            assertThat(updated.getEmail()).isEqualTo(newEmail);
            verify(emailVerificationService).assertVerifiedAndConsume(newEmail);
        }

        @Test
        @DisplayName("프로필 수정 실패 - 현재 로그인 아이디가 null")
        void updateProfile_Fail_NullCurrentLoginId() {
            // when & then
            assertThatThrownBy(() -> userService.updateProfile(null, "password", "newid", null, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 사용자를 찾을 수 없음")
        void updateProfile_Fail_UserNotFound() {
            // given
            String currentLoginId = "nonexistent";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, "password", "newid", null, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 변경할 항목 없음")
        void updateProfile_Fail_NoChanges() {
            // given
            String currentLoginId = "testuser";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, "password", null, null, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 잘못된 비밀번호")
        void updateProfile_Fail_InvalidPassword() {
            // given
            String currentLoginId = "testuser";
            String wrongPassword = "wrongpassword";
            String newNickname = "새닉네임";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(wrongPassword, testUser.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, wrongPassword, null, null, newNickname))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_PASSWORD);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 비밀번호가 null")
        void updateProfile_Fail_NullPassword() {
            // given
            String currentLoginId = "testuser";
            String newNickname = "새닉네임";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, null, null, null, newNickname))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_PASSWORD);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 중복된 로그인 아이디")
        void updateProfile_Fail_DuplicateLoginId() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newLoginId = "existinguser";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(userRepository.existsByLoginId(newLoginId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, currentPassword, newLoginId, null, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.DUPLICATE_LOGIN_ID);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 중복된 이메일")
        void updateProfile_Fail_DuplicateEmail() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newEmail = "existing@example.com";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(userRepository.existsByEmail(newEmail)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, currentPassword, null, newEmail, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.DUPLICATE_EMAIL);
                    });
        }

        @Test
        @DisplayName("프로필 수정 실패 - 이메일 인증 미완료")
        void updateProfile_Fail_EmailVerificationRequired() {
            // given
            String currentLoginId = "testuser";
            String currentPassword = "password123";
            String newEmail = "newemail@example.com";

            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(currentPassword, testUser.getPassword())).willReturn(true);
            given(userRepository.existsByEmail(newEmail)).willReturn(false);
            doThrow(new CoreException(ErrorType.EMAIL_VERIFICATION_REQUIRED))
                    .when(emailVerificationService).assertVerifiedAndConsume(newEmail);

            // when & then
            assertThatThrownBy(() -> userService.updateProfile(currentLoginId, currentPassword, null, newEmail, null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.EMAIL_VERIFICATION_REQUIRED);
                    });
        }
    }

    @Nested
    @DisplayName("계정 삭제 테스트")
    class DeleteAccountTest {

        @Test
        @DisplayName("계정 삭제 성공")
        void deleteAccount_Success() {
            // given
            String currentLoginId = "testuser";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));

            // when
            userService.deleteAccount(currentLoginId);

            // then
            verify(tokenStore).deleteRefreshToken(testUser.getLoginId());
            verify(tokenStore).deletePasswordResetCode(testUser.getId());
            verify(emailVerificationService).deleteByEmail(testUser.getEmail());
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("계정 삭제 실패 - 현재 로그인 아이디가 null")
        void deleteAccount_Fail_NullCurrentLoginId() {
            // when & then
            assertThatThrownBy(() -> userService.deleteAccount(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });
        }

        @Test
        @DisplayName("계정 삭제 실패 - 사용자를 찾을 수 없음")
        void deleteAccount_Fail_UserNotFound() {
            // given
            String currentLoginId = "nonexistent";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteAccount(currentLoginId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("프로필 조회 테스트")
    class GetProfileTest {

        @Test
        @DisplayName("프로필 조회 성공")
        void getProfile_Success() {
            // given
            String currentLoginId = "testuser";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.of(testUser));

            // when
            User result = userService.getProfile(currentLoginId);

            // then
            assertThat(result).isEqualTo(testUser);
            assertThat(result.getLoginId()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("프로필 조회 실패 - 현재 로그인 아이디가 null")
        void getProfile_Fail_NullCurrentLoginId() {
            // when & then
            assertThatThrownBy(() -> userService.getProfile(null))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT_VALUE);
                    });
        }

        @Test
        @DisplayName("프로필 조회 실패 - 사용자를 찾을 수 없음")
        void getProfile_Fail_UserNotFound() {
            // given
            String currentLoginId = "nonexistent";
            given(userRepository.findByLoginId(currentLoginId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getProfile(currentLoginId))
                    .isInstanceOf(CoreException.class)
                    .satisfies(ex -> {
                        CoreException coreException = (CoreException) ex;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.USER_NOT_FOUND);
                    });
        }
    }
}
