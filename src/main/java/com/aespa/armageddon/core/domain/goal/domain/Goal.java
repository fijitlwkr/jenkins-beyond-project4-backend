package com.aespa.armageddon.core.domain.goal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal")
@Getter
@NoArgsConstructor
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType goalType; // SAVING / EXPENSE

    private String title;

    @Column(nullable = false)
    private Integer targetAmount;

    private LocalDate startDate;
    private LocalDate endDate;

    // 지출 목표 전용
    @Enumerated(EnumType.STRING)
    private ExpenseCategory expenseCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    /* ================= 생성 ================= */

    public static Goal createSavingGoal(
            Long userId,
            String title,
            Integer targetAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Goal goal = new Goal();
        goal.userId = userId;
        goal.goalType = GoalType.SAVING;
        goal.title = "[저축] " + title;
        goal.targetAmount = targetAmount;
        goal.startDate = startDate;
        goal.endDate = endDate;
        goal.status = GoalStatus.ACTIVE;
        goal.createdAt = LocalDateTime.now();
        goal.updatedAt = LocalDateTime.now();
        return goal;
    }

    public static Goal createExpenseGoal(
            Long userId,
            ExpenseCategory category,
            String title,
            Integer targetAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Goal goal = new Goal();
        goal.userId = userId;
        goal.goalType = GoalType.EXPENSE;
        goal.title = "[지출] " + title;
        goal.expenseCategory = category;
        goal.targetAmount = targetAmount;
        goal.startDate = startDate;
        goal.endDate = endDate;
        goal.status = GoalStatus.ACTIVE;
        goal.createdAt = LocalDateTime.now();
        goal.updatedAt = LocalDateTime.now();
        return goal;
    }

    /* ================= 수정 ================= */

    public void updateTarget(
            String title,
            Integer targetAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (this.status != GoalStatus.ACTIVE) {
            throw new IllegalStateException("진행 중인 목표만 수정할 수 있습니다.");
        }

        if (title != null && !title.isBlank()) {
            this.title = title;
        }

        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    /* ================= 상태 전이 ================= */

    /**
     * 현재 금액을 기준으로 상태 갱신
     * - SAVING
     *   - current >= target → COMPLETED
     *   - 기간 만료 & 미달 → FAILED
     * - EXPENSE
     *   - current > target → EXCEEDED
     *   - 기간 만료 & 이하 → SUCCESS
     */
    public void updateStatus(int currentAmount) {
        if (this.status != GoalStatus.ACTIVE) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (goalType == GoalType.SAVING) {
            if (currentAmount >= targetAmount) {
                complete();
                return;
            }

            if (today.isAfter(endDate)) {
                failSavingGoal();
            }
            return;
        }

        // EXPENSE
        if (currentAmount > targetAmount) {
            exceedExpenseGoal();
            return;
        }

        if (today.isAfter(endDate)) {
            succeedExpenseGoal();
        }
    }

    /* ================= 내부 전용 ================= */

    private void complete() {
        this.status = GoalStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void failSavingGoal() {
        if (this.goalType != GoalType.SAVING) {
            throw new IllegalStateException("저축 목표만 실패 처리할 수 있습니다.");
        }
        this.status = GoalStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    private void exceedExpenseGoal() {
        if (this.goalType != GoalType.EXPENSE) {
            throw new IllegalStateException("지출 목표만 초과 처리할 수 있습니다.");
        }
        this.status = GoalStatus.EXCEEDED;
        this.updatedAt = LocalDateTime.now();
    }

    private void succeedExpenseGoal() {
        if (this.goalType != GoalType.EXPENSE) {
            throw new IllegalStateException("지출 목표만 성공 처리할 수 있습니다.");
        }
        this.status = GoalStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = GoalStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }
}