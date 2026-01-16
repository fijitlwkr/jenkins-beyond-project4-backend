package com.aespa.armageddon.core.domain.cashflow.repository;

import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.cashflow.dto.IncomeExpenseSum;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatisticsRepository {

    IncomeExpenseSum findIncomeExpenseSum(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    );

    List<CategoryExpenseSum> findCategoryExpenseSum(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    );

    List<TopExpenseItemResponse> findTopExpenseItems(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            int limit
    );

    /**
     * 지출 추이 통계 (DAY / WEEK / MONTH)
     * @param unit DAY | WEEK | MONTH
     */
    List<ExpenseTrendRawDto> findExpenseTrend(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            TrendUnit unit
    );

}