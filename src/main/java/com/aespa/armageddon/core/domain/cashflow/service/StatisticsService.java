package com.aespa.armageddon.core.domain.cashflow.service;

import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.cashflow.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    public SummaryStatisticsResponse getSummary(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        IncomeExpenseSum sum =
                statisticsRepository.findIncomeExpenseSum(userNo, startDate, endDate);

        long income = sum.totalIncome();
        long expense = sum.totalExpense();

        long netProfit = income - expense;

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long averageExpense = days > 0 ? expense / days : 0;

        return new SummaryStatisticsResponse(
                income,
                expense,
                netProfit,
                averageExpense
        );
    }

    public List<CategoryExpenseRatio> getCategoryExpenseWithRatio(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<CategoryExpenseSum> sums =
                statisticsRepository.findCategoryExpenseSum(
                        userNo, startDate, endDate
                );

        long totalExpense = sums.stream()
                .mapToLong(CategoryExpenseSum::totalExpense)
                .sum();

        if (totalExpense == 0) {
            return List.of();
        }

        return sums.stream()
                .map(sum -> new CategoryExpenseRatio(
                        sum.category(),
                        sum.totalExpense(),
                        (double) sum.totalExpense() * 100 / totalExpense
                ))
                .toList();
    }

    public List<TopExpenseItemResponse> getTopExpenseItems(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit
    ) {
        // 기본 기간: 이번 달
        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        // 기본 limit
        int resultLimit = (limit == null || limit <= 0) ? 5 : limit;

        return statisticsRepository.findTopExpenseItems(
                userNo,
                startDate,
                endDate,
                resultLimit
        );
    }

    //추이 관련 파트
    public ExpenseTrendResponse getExpenseTrend(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            TrendUnit unit
    ) {
        List<ExpenseTrendRawDto> raws =
                statisticsRepository.findExpenseTrend(userNo, startDate, endDate, unit);

        Map<String, Long> aggregated = new LinkedHashMap<>();

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        WeekFields wf = WeekFields.ISO;

        for (ExpenseTrendRawDto raw : raws) {
            String key = switch (unit) {
                case DAY -> raw.date().format(dayFmt);
                case WEEK -> {
                    int week = raw.date().get(wf.weekOfWeekBasedYear());
                    yield raw.date().getYear() + "-W" + String.format("%02d", week);
                }
                case MONTH -> raw.date().format(monthFmt);
            };

            aggregated.merge(key, raw.amount(), Long::sum);
        }

        List<ExpenseTrendPoint> data = aggregated.entrySet().stream()
                .map(e -> new ExpenseTrendPoint(e.getKey(), e.getValue()))
                .toList();

        return new ExpenseTrendResponse(unit, data);
    }

}
