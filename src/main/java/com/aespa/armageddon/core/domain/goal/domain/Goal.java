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

    // 저축 목표: 사용
    // 지출 목표: 월 시작/종료일 자동 세팅
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

    /* ================= 생성/수정 로직 ================= */

    public static Goal createSavingGoal(
            Long userId,
            String title,
            Integer targetAmount,
            LocalDate startDate,
            LocalDate endDate) {
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
            LocalDate endDate) {
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

    public void updateTarget(String title, Integer targetAmount, LocalDate startDate, LocalDate endDate) {
        if (this.status == GoalStatus.COMPLETED || this.status == GoalStatus.FAILED) {
            throw new IllegalStateException("완료되거나 실패한 목표는 수정할 수 없습니다.");
        }

        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void checkStatus(int progressRate) {
        if (this.status != GoalStatus.ACTIVE) {
            return;
        }

        // 100% 달성 시 완료 처리
        if (progressRate >= 100) {
            complete();
            return;
        }

        // 기간 만료 시 처리
        if (LocalDate.now().isAfter(endDate)) {
            fail();
        }
    }

    private void complete() {
        this.status = GoalStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void fail() {
        this.status = GoalStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = GoalStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

}
