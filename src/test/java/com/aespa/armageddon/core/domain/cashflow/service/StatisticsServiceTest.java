package com.aespa.armageddon.core.domain.cashflow.service;

import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.cashflow.repository.StatisticsRepository;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

        @InjectMocks
        StatisticsService statisticsService;

        @Mock
        StatisticsRepository statisticsRepository;

        @Test
        @DisplayName("요약 통계: 순이익과 평균 지출이 정확히 계산되어야 한다")
        void getSummaryTest() {
                // given
                Long userNo = 1L;
                LocalDate start = LocalDate.of(2024, 11, 1);
                LocalDate end = LocalDate.of(2024, 11, 30); // 30일

                // 수입 100만, 지출 40만 -> 순수익 60만, 평균지출 40만/30일 = 13333...
                given(statisticsRepository.findIncomeExpenseSum(userNo, start, end))
                                .willReturn(new IncomeExpenseSum(1000000L, 400000L));

                // when
                SummaryStatisticsResponse result = statisticsService.getSummary(userNo, start, end);

                // then
                assertThat(result.totalIncome()).isEqualTo(1000000L);
                assertThat(result.totalExpense()).isEqualTo(400000L);
                assertThat(result.netProfit()).isEqualTo(600000L);

                long expectedAvg = 400000L / 30L; // 13333
                assertThat(result.averageDailyExpense()).isEqualTo(expectedAvg);
        }

        @Test
        @DisplayName("카테고리 비율: 전체 지출 대비 비율이 퍼센트로 계산되어야 한다")
        void getCategoryExpenseWithRatioTest() {
                // given
                Long userNo = 1L;
                LocalDate start = LocalDate.now();
                LocalDate end = LocalDate.now();

                // 총 지출 10,000원
                // FOOD: 5,000 (50%)
                // SHOPPING: 3,000 (30%)
                // OTHER: 2,000 (20%)
                List<CategoryExpenseSum> mockSums = List.of(
                                new CategoryExpenseSum(Category.FOOD, 5000L),
                                new CategoryExpenseSum(Category.SHOPPING, 3000L),
                                new CategoryExpenseSum(Category.EDUCATION, 2000L));

                given(statisticsRepository.findCategoryExpenseSum(userNo, start, end))
                                .willReturn(mockSums);

                // when
                List<CategoryExpenseRatio> result = statisticsService.getCategoryExpenseWithRatio(userNo, start, end);

                // then
                assertThat(result).hasSize(3);

                CategoryExpenseRatio food = result.stream().filter(r -> r.category() == Category.FOOD).findFirst()
                                .get();
                assertThat(food.ratio()).isEqualTo(50.0);

                CategoryExpenseRatio shopping = result.stream().filter(r -> r.category() == Category.SHOPPING)
                                .findFirst()
                                .get();
                assertThat(shopping.ratio()).isEqualTo(30.0);
        }

        @Test
        @DisplayName("지출 항목 TOP 조회: limit 값이 없으면 기본 5개로 동작해야 한다")
        void getTopExpenseItemsDefaultLimitTest() {
                // given
                Long userNo = 1L;
                LocalDate start = LocalDate.now();
                LocalDate end = LocalDate.now();

                given(statisticsRepository.findTopExpenseItems(eq(userNo), eq(start), eq(end), eq(5)))
                                .willReturn(List.of()); // 결과는 중요하지 않음, 호출 인자 검증용

                // when
                statisticsService.getTopExpenseItems(userNo, start, end, null);

                // then
                // verify Mock
                org.mockito.Mockito.verify(statisticsRepository).findTopExpenseItems(userNo, start, end, 5);
        }

        @Test
        @DisplayName("지출 추이: 데이터가 날짜 포맷에 맞게 가공되어야 한다 (DAY)")
        void getExpenseTrendDayTest() {
                // given
                Long userNo = 1L;
                LocalDate start = LocalDate.of(2024, 1, 1);
                LocalDate end = LocalDate.of(2024, 1, 2);

                List<ExpenseTrendRawDto> mockRaws = List.of(
                                new ExpenseTrendRawDto(LocalDate.of(2024, 1, 1), 1000L),
                                new ExpenseTrendRawDto(LocalDate.of(2024, 1, 2), 2000L));

                given(statisticsRepository.findExpenseTrend(userNo, start, end, TrendUnit.DAY))
                                .willReturn(mockRaws);

                // when
                ExpenseTrendResponse result = statisticsService.getExpenseTrend(userNo, start, end, TrendUnit.DAY);

                // then
                assertThat(result.getUnit()).isEqualTo(TrendUnit.DAY);
                assertThat(result.getData()).hasSize(2);
                assertThat(result.getData().get(0).getLabel()).isEqualTo("2024-01-01");
                assertThat(result.getData().get(0).getAmount()).isEqualTo(1000L);
        }
}
