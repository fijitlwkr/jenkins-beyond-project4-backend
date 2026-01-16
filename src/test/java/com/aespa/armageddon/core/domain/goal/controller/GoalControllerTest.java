package com.aespa.armageddon.core.domain.goal.controller;

import com.aespa.armageddon.core.common.support.error.GlobalExceptionHandler;
import com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory;
import com.aespa.armageddon.core.domain.goal.domain.GoalStatus;
import com.aespa.armageddon.core.domain.goal.domain.GoalType;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateExpenseGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateSavingGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.UpdateGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalDetailResponse;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalResponse;
import com.aespa.armageddon.core.domain.goal.service.GoalService;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalController 테스트")
class GoalControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private GoalController goalController;

    @Mock
    private GoalService goalService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private ObjectMapper objectMapper;



    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(goalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Nested
    @DisplayName("목표 전체 조회 API 테스트")
    class GetGoalsApiTest {

        @Test
        @DisplayName("목표 전체 조회 성공")
        void getGoals_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            LocalDate now = LocalDate.now();
            LocalDate endDate = now.plusDays(30);

            GoalResponse goalResponse = new GoalResponse(
                    1L,
                    GoalType.SAVING,
                    "테스트 목표",
                    100000,
                    50,
                    GoalStatus.ACTIVE,
                    null,
                    now,
                    endDate);
            List<GoalResponse> goalResponses = Collections.singletonList(goalResponse);

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            given(goalService.getGoals(userId)).willReturn(goalResponses);

            // when & then
            mockMvc.perform(get("/api/goals")
                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0].goalId").value(1L))
                    .andExpect(jsonPath("$.data[0].goalType").value("SAVING"));
        }
    }


    @Nested
    @DisplayName("목표 세부정보 조회 API 테스트")
    class GetGoalDetailApiTest {

        @Test
        @DisplayName("목표 세부정보 조회 성공")
        void getGoalDetail_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            Long goalId = 1L;
            LocalDate now = LocalDate.now();
            LocalDate endDate = now.plusDays(30);

            GoalDetailResponse goalDetailResponse = new GoalDetailResponse(
                    1L,
                    "테스트 목표",
                    GoalType.SAVING,
                    100000,
                    50000,
                    50,
                    100000,
                    "화이팅!",
                    now,
                    endDate,
                    GoalStatus.ACTIVE,
                    null);

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            given(goalService.getGoalDetail(userId, goalId)).willReturn(goalDetailResponse);

            // when & then
            mockMvc.perform(get("/api/goals/{goalId}", goalId)
                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.goalId").value(1L))
                    .andExpect(jsonPath("$.data.progressRate").value(50));
        }
    }

    @Nested
    @DisplayName("저축 목표 생성 API 테스트")
    class CreateSavingGoalApiTest {

        @Test
        @DisplayName("저축 목표 생성 성공")
        void createSavingGoal_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            LocalDate now = LocalDate.now();
            LocalDate endDate = now.plusDays(30);

            CreateSavingGoalRequest request = new CreateSavingGoalRequest(
                    "저축 목표",
                    100000,
                    now,
                    endDate);

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            doNothing().when(goalService).createSavingGoal(anyLong(), any(CreateSavingGoalRequest.class));

            // when & then
            mockMvc.perform(post("/api/goals/saving")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(goalService).createSavingGoal(anyLong(), any(CreateSavingGoalRequest.class));
        }
    }

    @Nested
    @DisplayName("지출 목표 생성 API 테스트")
    class CreateExpenseGoalApiTest {

        @Test
        @DisplayName("지출 목표 생성 성공")
        void createExpenseGoal_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            LocalDate now = LocalDate.now();
            LocalDate endDate = now.plusDays(30);

            CreateExpenseGoalRequest request = new CreateExpenseGoalRequest(
                    "지출 목표",
                    ExpenseCategory.FOOD,
                    50000,
                    now,
                    endDate);

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            doNothing().when(goalService).createExpenseGoal(anyLong(), any(CreateExpenseGoalRequest.class));

            // when & then
            mockMvc.perform(post("/api/goals/expense")
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(goalService).createExpenseGoal(anyLong(), any(CreateExpenseGoalRequest.class));
        }
    }

    @Nested
    @DisplayName("목표 수정 API 테스트")
    class UpdateGoalApiTest {

        @Test
        @DisplayName("목표 수정 성공")
        void updateGoal_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            Long goalId = 1L;
            LocalDate now = LocalDate.now();
            LocalDate endDate = now.plusDays(30);

            UpdateGoalRequest request = new UpdateGoalRequest(
                    "수정된 목표",
                    200000,
                    now,
                    endDate);

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            doNothing().when(goalService).updateGoal(anyLong(), anyLong(), any(UpdateGoalRequest.class));

            // when & then
            mockMvc.perform(put("/api/goals/{goalId}", goalId)
                    .header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(goalService).updateGoal(anyLong(), anyLong(), any(UpdateGoalRequest.class));
        }
    }

    @Nested
    @DisplayName("목표 삭제 API 테스트")
    class DeleteGoalApiTest {

        @Test
        @DisplayName("목표 삭제 성공")
        void deleteGoal_Success() throws Exception {
            // given
            String token = "Bearer testToken";
            Long userId = 1L;
            Long goalId = 1L;

            given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
            doNothing().when(goalService).deleteGoal(userId, goalId);

            // when & then
            mockMvc.perform(delete("/api/goals/{goalId}", goalId)
                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("SUCCESS"));

            verify(goalService).deleteGoal(userId, goalId);
        }
    }
}
