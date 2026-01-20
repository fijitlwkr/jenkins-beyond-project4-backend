package com.aespa.armageddon.core.domain.goal.service;

import com.aespa.armageddon.core.domain.goal.domain.*;
import com.aespa.armageddon.core.domain.goal.dto.request.*;
import com.aespa.armageddon.core.domain.goal.dto.response.*;
import com.aespa.armageddon.core.domain.goal.port.TransactionPort;
import com.aespa.armageddon.core.domain.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final TransactionPort transactionPort;

    /* ===================== 조회 ===================== */

    @Transactional
    public List<GoalResponse> getGoals(Long userId) {
        List<Goal> goals = goalRepository.findByUserIdAndStatusNot(userId, GoalStatus.DELETED);

        // 상태 최신화
        for (Goal goal : goals) {
            refreshGoalStatus(goal);
        }

        return goals.stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Transactional
    public GoalDetailResponse getGoalDetail(Long userId, Long goalId) {
        Goal goal = findGoal(userId, goalId);

        // 상태 최신화
        refreshGoalStatus(goal);

        int currentAmount = getCurrentAmount(goal);
        int progressRate = calculateRate(goal, currentAmount);

        return new GoalDetailResponse(
                goal.getGoalId(),
                goal.getTitle(),
                goal.getGoalType(),
                goal.getTargetAmount(),
                currentAmount,
                progressRate,
                createStatusMessage(goal),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.getStatus(),
                goal.getExpenseCategory());
    }

    /* ===================== 생성 ===================== */

    public void createSavingGoal(Long userId, CreateSavingGoalRequest request) {
        validateDuplicateSavingGoal(userId);

        Goal goal = Goal.createSavingGoal(
                userId,
                request.title(),
                request.targetAmount(),
                request.startDate(),
                request.endDate());

        goalRepository.save(goal);
    }

    public void createExpenseGoal(Long userId, CreateExpenseGoalRequest request) {
        validateDuplicateExpenseGoal(userId, request.category());

        Goal goal = Goal.createExpenseGoal(
                userId,
                request.category(),
                request.title(),
                request.targetAmount(),
                request.startDate(),
                request.endDate());

        goalRepository.save(goal);
    }

    /* ===================== 수정 / 삭제 ===================== */

    public void updateGoal(Long userId, Long goalId, UpdateGoalRequest request) {
        Goal goal = findGoal(userId, goalId);
        goal.updateTarget(
                request.title(),
                request.targetAmount(),
                request.startDate(),
                request.endDate());
    }

    public void deleteGoal(Long userId, Long goalId) {
        Goal goal = findGoal(userId, goalId);
        goal.delete();
    }

    /* ===================== 상태 갱신 (유일한 변경 지점) ===================== */

    public void refreshGoalStatus(Goal goal) {
        int currentAmount = getCurrentAmount(goal);
        goal.updateStatus(currentAmount);
    }

    /* ===================== 내부 로직 ===================== */

    private Goal findGoal(Long userId, Long goalId) {
        return goalRepository.findByGoalIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("목표를 찾을 수 없습니다."));
    }

    private int getCurrentAmount(Goal goal) {
        Long sum = transactionPort.getTransactionSum(
                goal.getUserId(),
                goal.getGoalType(),
                goal.getExpenseCategory(),
                goal.getStartDate(),
                goal.getEndDate());
        return sum == null ? 0 : sum.intValue();
    }

    private int calculateRate(Goal goal, int currentAmount) {
        if (goal.getTargetAmount() == 0)
            return 0;

        int rate = (currentAmount * 100) / goal.getTargetAmount();

        // ✔️ 지출 목표는 100% 초과 허용
        if (goal.getGoalType() == GoalType.EXPENSE) {
            return rate;
        }

        return Math.min(rate, 100);
    }

    private String createStatusMessage(Goal goal) {
        return switch (goal.getStatus()) {
            case COMPLETED -> goal.getGoalType() == GoalType.EXPENSE
                    ? "이번 달 지출 목표를 잘 지켰어요!"
                    : "축하합니다! 목표를 달성했어요!";
            case FAILED -> "아쉽게도 목표 달성에 실패했어요";
            case EXCEEDED -> "지출 목표 금액을 초과했어요";
            case ACTIVE -> goal.getGoalType() == GoalType.EXPENSE
                    ? ""
                    : "목표를 향해 진행 중이에요";
            default -> "";
        };
    }

    private GoalResponse toGoalResponse(Goal goal) {
        int current = getCurrentAmount(goal);
        int rate = calculateRate(goal, current);

        return new GoalResponse(
                goal.getGoalId(),
                goal.getGoalType(),
                goal.getTitle(),
                goal.getTargetAmount(),
                current,
                rate,
                goal.getStatus(),
                createStatusMessage(goal),
                goal.getExpenseCategory(),
                goal.getStartDate(),
                goal.getEndDate());
    }

    /* ===================== 검증 ===================== */

    private void validateDuplicateSavingGoal(Long userId) {
        if (goalRepository.existsByUserIdAndGoalTypeAndStatus(
                userId, GoalType.SAVING, GoalStatus.ACTIVE)) {
            throw new IllegalStateException("이미 진행 중인 저축 목표가 있습니다.");
        }
    }

    private void validateDuplicateExpenseGoal(Long userId, ExpenseCategory category) {
        if (goalRepository.existsByUserIdAndGoalTypeAndExpenseCategoryAndStatus(
                userId, GoalType.EXPENSE, category, GoalStatus.ACTIVE)) {
            throw new IllegalStateException("해당 카테고리에 이미 진행 중인 지출 목표가 있습니다.");
        }
    }
}