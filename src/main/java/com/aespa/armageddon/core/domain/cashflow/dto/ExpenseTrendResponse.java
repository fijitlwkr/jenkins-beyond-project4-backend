package com.aespa.armageddon.core.domain.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExpenseTrendResponse {

    /**
     * DAY / WEEK / MONTH
     */
    private TrendUnit unit;

    /**
     * 추이 데이터 (정렬된 상태)
     */
    private List<ExpenseTrendPoint> data;
}
