package com.aespa.armageddon.core.domain.goal.service;

import com.aespa.armageddon.core.domain.goal.domain.*;
import com.aespa.armageddon.core.domain.goal.dto.request.*;
import com.aespa.armageddon.core.domain.goal.dto.response.*;
import com.aespa.armageddon.core.domain.goal.port.TransactionPort;
import com.aespa.armageddon.core.domain.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final TransactionPort transactionPort;

    /* ===================== 조회 ===================== */

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(Long userId) {
        return goalRepository.findByUserIdAndStatusNot(userId, GoalStatus.DELETED)
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    // 상세 조회 시 상태 업데이트 로직이 포함되므로 readOnly = true 제거 혹은 별도 처리 필요
    // 하지만 JPA Dirty Checking을 이용하려면 트랜잭션 내에서 엔티티 변경이 일어나야 함
    public GoalDetailResponse getGoalDetail(Long userId, Long goalId) {
        Goal goal = findGoal(userId, goalId);

        int currentAmount = getCurrentAmount(goal);
        int progressRate = calculateRate(currentAmount, goal.getTargetAmount());

        // 상태 체크 및 업데이트 (Dirty Checking)
        goal.checkStatus(progressRate);

        Integer expectedAmount = calculateExpectedAmount(goal, currentAmount);

        return new GoalDetailResponse(
                goal.getGoalId(),
                goal.getTitle(),
                goal.getGoalType(),
                goal.getTargetAmount(),
                currentAmount,
                progressRate,
                expectedAmount,
                createStatusMessage(goal, currentAmount, expectedAmount),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.getStatus(),
                goal.getExpenseCategory());
    }

    /* ===================== 생성 ===================== */

    public void createSavingGoal(Long userId, CreateSavingGoalRequest request) {
        if (goalRepository.existsByUserIdAndGoalTypeAndStatus(userId, GoalType.SAVING, GoalStatus.ACTIVE)) {
            throw new IllegalStateException("이미 진행 중인 저축 목표가 있습니다.");
        }

        Goal goal = Goal.createSavingGoal(
                userId,
                request.title(),
                request.targetAmount(),
                request.startDate(),
                request.endDate());
        goalRepository.save(goal);
    }

    public void createExpenseGoal(Long userId, CreateExpenseGoalRequest request) {
        if (goalRepository.existsByUserIdAndGoalTypeAndExpenseCategoryAndStatus(userId, GoalType.EXPENSE,
                request.category(), GoalStatus.ACTIVE)) {
            throw new IllegalStateException("해당 카테고리에 이미 진행 중인 지출 목표가 있습니다.");
        }

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
        goal.updateTarget(request.title(), request.targetAmount(), request.startDate(), request.endDate());
    }

    public void deleteGoal(Long userId, Long goalId) {
        Goal goal = findGoal(userId, goalId);
        goalRepository.delete(goal);
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
        return sum.intValue();
    }

    private int calculateRate(int current, int target) {
        if (target == 0)
            return 0;
        return Math.min((current * 100) / target, 100);
    }

    private Integer calculateExpectedAmount(Goal goal, int currentAmount) {
        if (goal.getGoalType() != GoalType.SAVING)
            return null;
        if (goal.getStatus() != GoalStatus.ACTIVE)
            return null; // 완료/실패된 목표는 예측 불필요

        long totalDays = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getEndDate()) + 1;
        long passedDays = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now()) + 1;

        if (passedDays <= 0)
            return null;

        int dailyAverage = (int) (currentAmount / passedDays);
        return dailyAverage * (int) totalDays;
    }

    private String createStatusMessage(Goal goal, int current, Integer expected) {
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            return "축하합니다! 목표를 달성했어요!";
        }
        if (goal.getStatus() == GoalStatus.FAILED) {
            return "아쉽게도 목표 달성에 실패했어요.";
        }

        if (goal.getGoalType() == GoalType.EXPENSE) {
            int remaining = goal.getTargetAmount() - current;
            if (remaining < 0)
                return "이번 달 목표를 초과했어요";

            double rate = (double) current / goal.getTargetAmount();
            if (rate >= 0.8)
                return "목표 금액에 가까워지고 있어요. 주의하세요!";

            return "아직 여유가 있어요";
        }

        if (expected == null)
            return "조금 더 지켜볼게요";

        int diff = expected - goal.getTargetAmount();
        if (diff > 0)
            return "현재 속도라면 목표보다 더 모을 수 있어요";
        if (diff < 0)
            return "현재 속도라면 목표 금액에 조금 못 미칠 수 있어요";
        return "현재 페이스로 목표 달성이 가능해요";
    }

    private GoalResponse toGoalResponse(Goal goal) {
        int current = getCurrentAmount(goal);
        int rate = calculateRate(current, goal.getTargetAmount());

        return new GoalResponse(
                goal.getGoalId(),
                goal.getGoalType(),
                goal.getTitle(),
                goal.getTargetAmount(),
                rate,
                goal.getStatus(),
                goal.getExpenseCategory(),
                goal.getStartDate(),
                goal.getEndDate());
    }
}
