package com.aespa.armageddon.core.domain.goal.service;

import com.aespa.armageddon.core.domain.goal.domain.*;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateExpenseGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateSavingGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.UpdateGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalDetailResponse;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalResponse;
import com.aespa.armageddon.core.domain.goal.port.TransactionPort;
import com.aespa.armageddon.core.domain.goal.repository.GoalRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private Goal expenseGoal;

    @BeforeEach
    void setUp() {
        savingGoal = Goal.createSavingGoal(
                1L,
                "저축 목표",
                1_000_000,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(20)
        );

        expenseGoal = Goal.createExpenseGoal(
                1L,
                ExpenseCategory.FOOD,
                "식비 줄이기",
                300_000,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(5)
        );
    }

    /* ===================== 조회 ===================== */

    @Test
    @DisplayName("저축 목표 상세 조회 - 현재 금액 및 달성률 계산")
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
        assertThat(response.status()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(response.statusMessage()).isEqualTo("목표를 향해 진행 중이에요");
    }

    @Test
    @DisplayName("지출 목표 상세 조회 - 초과 지출 시 EXCEEDED 상태")
    void getExpenseGoalDetail_exceeded() {
        // given
        given(goalRepository.findByGoalIdAndUserId(1L, 1L))
                .willReturn(Optional.of(expenseGoal));

        given(transactionPort.getTransactionSum(
                anyLong(),
                eq(GoalType.EXPENSE),
                eq(ExpenseCategory.FOOD),
                any(),
                any()
        )).willReturn(400_000L);

        // when
        GoalDetailResponse response = goalService.getGoalDetail(1L, 1L);

        // then
        assertThat(response.currentAmount()).isEqualTo(400_000);
        assertThat(response.progressRate()).isEqualTo(133);
        assertThat(response.status()).isEqualTo(GoalStatus.EXCEEDED);
        assertThat(response.statusMessage()).isEqualTo("지출 목표 금액을 초과했어요");
    }

    @Test
    @DisplayName("전체 목표 조회 - 상태가 최신화된다")
    void getGoals_refreshStatus() {
        // given
        given(goalRepository.findByUserIdAndStatusNot(1L, GoalStatus.DELETED))
                .willReturn(List.of(savingGoal));

        given(transactionPort.getTransactionSum(
                anyLong(),
                any(),
                any(),
                any(),
                any()
        )).willReturn(1_000_000L);

        // when
        List<GoalResponse> responses = goalService.getGoals(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(GoalStatus.COMPLETED);
    }

    /* ===================== 생성 ===================== */

    @Test
    @DisplayName("저축 목표 생성 성공")
    void createSavingGoal_success() {
        // given
        given(goalRepository.existsByUserIdAndGoalTypeAndStatus(
                1L, GoalType.SAVING, GoalStatus.ACTIVE))
                .willReturn(false);

        CreateSavingGoalRequest request =
                new CreateSavingGoalRequest(
                        "새 저축",
                        500_000,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30)
                );

        // when & then
        assertThatCode(() -> goalService.createSavingGoal(1L, request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("저축 목표 생성 - 중복 시 예외")
    void createSavingGoal_duplicate() {
        // given
        given(goalRepository.existsByUserIdAndGoalTypeAndStatus(
                1L, GoalType.SAVING, GoalStatus.ACTIVE))
                .willReturn(true);

        CreateSavingGoalRequest request =
                new CreateSavingGoalRequest(
                        "저축",
                        100_000,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30)
                );

        // then
        assertThatThrownBy(() -> goalService.createSavingGoal(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 진행 중인 저축 목표");
    }

    @Test
    @DisplayName("지출 목표 생성 - 카테고리 중복 시 예외")
    void createExpenseGoal_duplicate() {
        // given
        given(goalRepository.existsByUserIdAndGoalTypeAndExpenseCategoryAndStatus(
                1L, GoalType.EXPENSE, ExpenseCategory.FOOD, GoalStatus.ACTIVE))
                .willReturn(true);

        CreateExpenseGoalRequest request =
                new CreateExpenseGoalRequest(
                        "식비",
                        ExpenseCategory.FOOD,
                        200_000,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30)
                );

        // then
        assertThatThrownBy(() -> goalService.createExpenseGoal(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 진행 중인 지출 목표");
    }

    /* ===================== 수정 / 삭제 ===================== */

    @Test
    @DisplayName("목표 수정 - 제목과 금액 변경")
    void updateGoal_success() {
        // given
        given(goalRepository.findByGoalIdAndUserId(1L, 1L))
                .willReturn(Optional.of(savingGoal));

        UpdateGoalRequest request =
                new UpdateGoalRequest(
                        "수정된 목표",
                        2_000_000,
                        LocalDate.now().minusDays(3),
                        LocalDate.now().plusDays(90)
                );

        // when
        goalService.updateGoal(1L, 1L, request);

        // then
        assertThat(savingGoal.getTitle()).isEqualTo("수정된 목표");
        assertThat(savingGoal.getTargetAmount()).isEqualTo(2_000_000);
    }

    @Test
    @DisplayName("목표 삭제 - 상태가 DELETED로 변경된다")
    void deleteGoal_success() {
        // given
        given(goalRepository.findByGoalIdAndUserId(1L, 1L))
                .willReturn(Optional.of(savingGoal));

        // when
        goalService.deleteGoal(1L, 1L);

        // then
        assertThat(savingGoal.getStatus()).isEqualTo(GoalStatus.DELETED);
    }

    /* ===================== 예외 ===================== */

    @Test
    @DisplayName("존재하지 않는 목표 조회 시 예외")
    void getGoalDetail_notFound() {
        // given
        given(goalRepository.findByGoalIdAndUserId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> goalService.getGoalDetail(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("목표를 찾을 수 없습니다");
    }
}