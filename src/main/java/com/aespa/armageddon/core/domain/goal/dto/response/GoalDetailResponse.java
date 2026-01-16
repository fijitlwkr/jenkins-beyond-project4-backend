package com.aespa.armageddon.core.domain.goal.dto.response;

import com.aespa.armageddon.core.domain.goal.domain.GoalType;
import com.aespa.armageddon.core.domain.goal.domain.GoalStatus;

import java.time.LocalDate;

public record GoalDetailResponse(
        Long goalId,
        String title,
        GoalType goalType,
        Integer targetAmount,
        Integer currentAmount, // 현재 저축 or 지출 금액
        Integer progressRate, // 달성률
        Integer expectedAmount, // 저축 목표만 (지출은 null)
        String statusMessage, // 동기부여 메시지
        LocalDate startDate,
        LocalDate endDate,
        GoalStatus status,
        com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory category) {
}