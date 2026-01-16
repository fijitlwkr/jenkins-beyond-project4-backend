package com.aespa.armageddon.core.api.user.controller;

import com.aespa.armageddon.core.api.user.dto.request.UserUpdateRequest;
import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.common.support.error.GlobalExceptionHandler;
import com.aespa.armageddon.core.domain.auth.entity.User;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new MockUserDetailsArgumentResolver())
                .build();

        testUser = User.builder()
                .loginId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("프로필 조회 API 테스트")
    class GetProfileApiTest {

        @Test
        @DisplayName("프로필 조회 성공")
        void getProfile_Success() throws Exception {
            // given
            given(userService.getProfile("testuser")).willReturn(testUser);

            // when & then
            mockMvc.perform(get("/api/users/me")
                            .principal(() -> "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.loginId").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터"));
        }

        @Test
        @DisplayName("프로필 조회 실패 - 사용자를 찾을 수 없음")
        void getProfile_Fail_UserNotFound() throws Exception {
            // given
            given(userService.getProfile("testuser"))
                    .willThrow(new CoreException(ErrorType.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/users/me")
                            .principal(() -> "testuser"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U001"));
        }

        @Test
        @DisplayName("프로필 조회 실패 - 잘못된 입력값")
        void getProfile_Fail_InvalidInputValue() throws Exception {
            // given
            given(userService.getProfile("testuser"))
                    .willThrow(new CoreException(ErrorType.INVALID_INPUT_VALUE));

            // when & then
            mockMvc.perform(get("/api/users/me")
                            .principal(() -> "testuser"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("C002"));
        }
    }

    @Nested
    @DisplayName("프로필 수정 API 테스트")
    class UpdateProfileApiTest {

        @Test
        @DisplayName("프로필 수정 성공 - 닉네임 변경")
        void updateProfile_Success_NicknameChange() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null, "새닉네임", "currentPassword");

            User updatedUser = User.builder()
                    .loginId("testuser")
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("새닉네임")
                    .build();
            ReflectionTestUtils.setField(updatedUser, "id", 1L);

            given(userService.updateProfile(eq("testuser"), eq("currentPassword"), isNull(), isNull(), eq("새닉네임")))
                    .willReturn(updatedUser);

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
        }

        @Test
        @DisplayName("프로필 수정 성공 - 로그인 아이디 변경")
        void updateProfile_Success_LoginIdChange() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("newloginid", null, null, "currentPassword");

            User updatedUser = User.builder()
                    .loginId("newloginid")
                    .email("test@example.com")
                    .password("encodedPassword")
                    .nickname("테스터")
                    .build();
            ReflectionTestUtils.setField(updatedUser, "id", 1L);

            given(userService.updateProfile(eq("testuser"), eq("currentPassword"), eq("newloginid"), isNull(), isNull()))
                    .willReturn(updatedUser);

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.loginId").value("newloginid"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 사용자를 찾을 수 없음")
        void updateProfile_Fail_UserNotFound() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null, "새닉네임", "currentPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U001"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 잘못된 비밀번호")
        void updateProfile_Fail_InvalidPassword() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null, "새닉네임", "wrongPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.INVALID_PASSWORD));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U004"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 중복된 로그인 아이디")
        void updateProfile_Fail_DuplicateLoginId() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("existinguser", null, null, "currentPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.DUPLICATE_LOGIN_ID));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U003"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 중복된 이메일")
        void updateProfile_Fail_DuplicateEmail() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, "existing@example.com", null, "currentPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.DUPLICATE_EMAIL));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U002"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 이메일 인증 미완료")
        void updateProfile_Fail_EmailVerificationRequired() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, "new@example.com", null, "currentPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.EMAIL_VERIFICATION_REQUIRED));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("A006"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 잘못된 입력값")
        void updateProfile_Fail_InvalidInputValue() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null, null, "currentPassword");

            given(userService.updateProfile(anyString(), anyString(), any(), any(), any()))
                    .willThrow(new CoreException(ErrorType.INVALID_INPUT_VALUE));

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("C002"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 유효성 검증 실패 (짧은 loginId)")
        void updateProfile_Fail_ValidationError_ShortLoginId() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest("ab", null, null, "currentPassword");

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }

        @Test
        @DisplayName("프로필 수정 실패 - 유효성 검증 실패 (빈 currentPassword)")
        void updateProfile_Fail_ValidationError_EmptyCurrentPassword() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest(null, null, "새닉네임", "");

            // when & then
            mockMvc.perform(put("/api/users/update")
                            .principal(() -> "testuser")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"));
        }
    }

    @Nested
    @DisplayName("계정 삭제 API 테스트")
    class DeleteAccountApiTest {

        @Test
        @DisplayName("계정 삭제 성공")
        void deleteAccount_Success() throws Exception {
            // given
            doNothing().when(userService).deleteAccount("testuser");

            // when & then
            mockMvc.perform(delete("/api/users/delete")
                            .principal(() -> "testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));
        }

        @Test
        @DisplayName("계정 삭제 실패 - 사용자를 찾을 수 없음")
        void deleteAccount_Fail_UserNotFound() throws Exception {
            // given
            doThrow(new CoreException(ErrorType.USER_NOT_FOUND))
                    .when(userService).deleteAccount("testuser");

            // when & then
            mockMvc.perform(delete("/api/users/delete")
                            .principal(() -> "testuser"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("U001"));
        }

        @Test
        @DisplayName("계정 삭제 실패 - 잘못된 입력값")
        void deleteAccount_Fail_InvalidInputValue() throws Exception {
            // given
            doThrow(new CoreException(ErrorType.INVALID_INPUT_VALUE))
                    .when(userService).deleteAccount("testuser");

            // when & then
            mockMvc.perform(delete("/api/users/delete")
                            .principal(() -> "testuser"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result").value("ERROR"))
                    .andExpect(jsonPath("$.error.code").value("C002"));
        }
    }
}
