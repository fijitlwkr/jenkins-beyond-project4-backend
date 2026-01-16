package com.aespa.armageddon.core.domain.goal.dto.request;

import java.time.LocalDate;

public record UpdateGoalRequest(
                String title,
                Integer targetAmount, // 수정할 목표 금액
                LocalDate startDate, // 시작일
                LocalDate endDate // 종료일
) {
}
