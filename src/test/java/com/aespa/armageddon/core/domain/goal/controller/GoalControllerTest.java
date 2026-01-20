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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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

    /* ===================== 목표 전체 조회 ===================== */

    @Test
    @DisplayName("목표 전체 조회 성공")
    void getGoals_success() throws Exception {
        // given
        String token = "Bearer testToken";
        Long userId = 1L;
        LocalDate now = LocalDate.now();

        GoalResponse response = new GoalResponse(
                1L,
                GoalType.SAVING,
                "저축 목표",
                100_000,
                50_000,
                50,
                GoalStatus.ACTIVE,
                "목표를 향해 진행 중이에요",
                null,
                now,
                now.plusDays(30)
        );

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
        given(goalService.getGoals(userId)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/goals")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].goalId").value(1L))
                .andExpect(jsonPath("$.data[0].goalType").value("SAVING"));
    }

    /* ===================== 목표 상세 조회 ===================== */

    @Test
    @DisplayName("목표 상세 조회 성공")
    void getGoalDetail_success() throws Exception {
        // given
        String token = "Bearer testToken";
        Long userId = 1L;
        Long goalId = 1L;
        LocalDate now = LocalDate.now();

        GoalDetailResponse response = new GoalDetailResponse(
                goalId,
                "저축 목표",
                GoalType.SAVING,
                100_000,
                50_000,
                50,
                "목표를 향해 진행 중이에요",
                now,
                now.plusDays(30),
                GoalStatus.ACTIVE,
                null
        );

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
        given(goalService.getGoalDetail(userId, goalId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/goals/{goalId}", goalId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.goalId").value(1L))
                .andExpect(jsonPath("$.data.progressRate").value(50));
    }

    /* ===================== 저축 목표 생성 ===================== */

    @Test
    @DisplayName("저축 목표 생성 성공")
    void createSavingGoal_success() throws Exception {
        // given
        String token = "Bearer testToken";
        Long userId = 1L;

        CreateSavingGoalRequest request = new CreateSavingGoalRequest(
                "저축 목표",
                100_000,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
        doNothing().when(goalService).createSavingGoal(anyLong(), any());

        // when & then
        mockMvc.perform(post("/api/goals/saving")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    /* ===================== 지출 목표 생성 ===================== */

    @Test
    @DisplayName("지출 목표 생성 성공")
    void createExpenseGoal_success() throws Exception {
        // given
        String token = "Bearer testToken";
        Long userId = 1L;

        CreateExpenseGoalRequest request = new CreateExpenseGoalRequest(
                "식비 줄이기",
                ExpenseCategory.FOOD,
                50_000,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(userId);
        doNothing().when(goalService).createExpenseGoal(anyLong(), any());

        // when & then
        mockMvc.perform(post("/api/goals/expense")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    /* ===================== 목표 수정 ===================== */

    @Test
    @DisplayName("목표 수정 성공")
    void updateGoal_success() throws Exception {
        // given
        String token = "Bearer testToken";

        UpdateGoalRequest request = new UpdateGoalRequest(
                "수정된 목표",
                200_000,
                LocalDate.now(),
                LocalDate.now().plusDays(60)
        );

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(1L);
        doNothing().when(goalService).updateGoal(anyLong(), anyLong(), any());

        // when & then
        mockMvc.perform(put("/api/goals/{goalId}", 1L)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    /* ===================== 목표 삭제 ===================== */

    @Test
    @DisplayName("목표 삭제 성공")
    void deleteGoal_success() throws Exception {
        // given
        String token = "Bearer testToken";

        given(jwtTokenProvider.getUserIdFromJWT(anyString())).willReturn(1L);
        doNothing().when(goalService).deleteGoal(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/goals/{goalId}", 1L)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }
}