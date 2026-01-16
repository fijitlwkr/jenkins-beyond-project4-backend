package com.aespa.armageddon.core.domain.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//추이 차트용
@Getter
@AllArgsConstructor
public class ExpenseTrendPoint {

    /**
     * DAY   → 2026-01-01
     * WEEK  → 2026-W01
     * MONTH → 2026-01
     */
    private String label;

    /**
     * 해당 기간의 지출 합계
     */
    private long amount;
}