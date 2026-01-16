package com.aespa.armageddon.core.domain.goal.dto.request;

import com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory;

public record CreateExpenseGoalRequest(
        String title, // 목표 이름
        ExpenseCategory category, // 지출 카테고리
        Integer targetAmount, // 목표 금액 (월 기준)
        java.time.LocalDate startDate, // 시작일
        java.time.LocalDate endDate // 종료일
) {
}