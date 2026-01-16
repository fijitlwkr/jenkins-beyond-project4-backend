package com.aespa.armageddon.core.domain.goal.service;

import com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory;
import com.aespa.armageddon.core.domain.goal.domain.Goal;
import com.aespa.armageddon.core.domain.goal.domain.GoalStatus;
import com.aespa.armageddon.core.domain.goal.domain.GoalType;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateSavingGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.UpdateGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalDetailResponse;
import com.aespa.armageddon.core.domain.goal.port.TransactionPort;
import com.aespa.armageddon.core.domain.goal.repository.GoalRepository;
import com.aespa.armageddon.core.domain.goal.service.GoalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private TransactionPort transactionPort;

    @InjectMocks
    private GoalService goalService;

    private Goal savingGoal;

    @BeforeEach
    void setUp() {
        savingGoal = Goal.createSavingGoal(
                1L,
                "저축 목표",
                1_000_000,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(20)
        );
    }

    @Test
    @DisplayName("저축 목표 상세 조회 - 달성률과 예측 금액 계산")
    void getSavingGoalDetail_success() {
        // given
        given(goalRepository.findByGoalIdAndUserId(1L, 1L))
                .willReturn(Optional.of(savingGoal));

        given(transactionPort.getTransactionSum(
                anyLong(),
                eq(GoalType.SAVING),
                isNull(),
                any(),
                any()
        )).willReturn(300_000L);

        // when
        GoalDetailResponse response = goalService.getGoalDetail(1L, 1L);

        // then
        assertThat(response.currentAmount()).isEqualTo(300_000);
        assertThat(response.progressRate()).isEqualTo(30);
        assertThat(response.expectedAmount()).isNotNull();
        assertThat(response.statusMessage()).contains("목표");
    }

    @Test
    @DisplayName("저축 목표 생성 - 이미 활성 목표 존재 시 예외")
    void createSavingGoal_duplicate() {
        // given
        given(goalRepository.existsByUserIdAndGoalTypeAndStatus(
                1L, GoalType.SAVING, GoalStatus.ACTIVE))
                .willReturn(true);

        CreateSavingGoalRequest request =
                new CreateSavingGoalRequest("저축", 100000, LocalDate.now(), LocalDate.now().plusDays(30));

        // then
        assertThatThrownBy(() -> goalService.createSavingGoal(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 진행 중인 저축 목표");
    }

    @Test
    @DisplayName("목표 수정 시 기존 데이터 유지 및 재계산")
    void updateGoal_recalculate() {
        // given
        given(goalRepository.findByGoalIdAndUserId(1L, 1L))
                .willReturn(Optional.of(savingGoal));

        UpdateGoalRequest request =
                new UpdateGoalRequest("수정", 2_000_000,
                        LocalDate.now().minusDays(5),
                        LocalDate.now().plusDays(60));

        // when
        goalService.updateGoal(1L, 1L, request);

        // then
        assertThat(savingGoal.getTargetAmount()).isEqualTo(2_000_000);
        assertThat(savingGoal.getTitle()).isEqualTo("수정");
    }
}
