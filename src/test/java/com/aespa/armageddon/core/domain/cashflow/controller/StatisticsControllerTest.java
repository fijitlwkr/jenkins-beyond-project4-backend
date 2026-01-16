package com.aespa.armageddon.core.domain.cashflow.controller;

import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.cashflow.service.StatisticsService;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatisticsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StatisticsService statisticsService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("요약 통계 조회 - 성공")
    void getSummaryStatistics() throws Exception {
        // given
        String token = "ValidToken";
        Long userNo = 1L;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        SummaryStatisticsResponse response = new SummaryStatisticsResponse(
                10000L, // totalIncome
                5000L, // totalExpense
                5000L, // netProfit
                166L // averageDailyExpense
        );

        given(jwtTokenProvider.getUserIdFromJWT(token)).willReturn(userNo);
        given(statisticsService.getSummary(eq(userNo), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/statistics/summary")
                .header("Authorization", "Bearer " + token)
                .param("startDate", start.toString())
                .param("endDate", end.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(10000))
                .andExpect(jsonPath("$.totalExpense").value(5000))
                .andExpect(jsonPath("$.netProfit").value(5000))
                .andExpect(jsonPath("$.averageDailyExpense").value(166));
    }

    @Test
    @DisplayName("카테고리별 지출 비율 조회 - 성공")
    void getCategoryExpenseStatistics() throws Exception {
        // given
        String token = "ValidToken";
        Long userNo = 1L;

        List<CategoryExpenseRatio> response = List.of(
                new CategoryExpenseRatio(Category.FOOD, 5000L, 50.0),
                new CategoryExpenseRatio(Category.TRANSPORT, 5000L, 50.0));

        given(jwtTokenProvider.getUserIdFromJWT(token)).willReturn(userNo);
        given(statisticsService.getCategoryExpenseWithRatio(eq(userNo), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/statistics/expense/categories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].ratio").value(50.0));
    }

    @Test
    @DisplayName("상위 지출 항목 조회 - 성공")
    void getTopExpenseItems() throws Exception {
        // given
        String token = "ValidToken";
        Long userNo = 1L;

        TopExpenseItemResponse item = new TopExpenseItemResponse(
                1L, "Expensive Item", 50000,
                Category.SHOPPING, LocalDate.of(2024, 1, 15)
        );

        given(jwtTokenProvider.getUserIdFromJWT(token)).willReturn(userNo);
        given(statisticsService.getTopExpenseItems(
                eq(userNo),
                any(LocalDate.class),
                any(LocalDate.class),
                anyInt()
        )).willReturn(List.of(item));

        // when & then
        mockMvc.perform(get("/api/statistics/expense/top")
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "5")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(1L))
                .andExpect(jsonPath("$[0].amount").value(50000));
    }


    @Test
    @DisplayName("지출 추이 조회 - 성공")
    void getExpenseTrend() throws Exception {
        // given
        String token = "ValidToken";
        Long userNo = 1L;
        TrendUnit unit = TrendUnit.DAY;

        ExpenseTrendResponse response = new ExpenseTrendResponse(
                unit,
                List.of(new ExpenseTrendPoint("2024-01-01", 1000L)));

        given(jwtTokenProvider.getUserIdFromJWT(token)).willReturn(userNo);
        given(statisticsService.getExpenseTrend(eq(userNo), any(LocalDate.class), any(LocalDate.class), eq(unit)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/statistics/expense/trend")
                .header("Authorization", "Bearer " + token)
                .param("unit", "DAY")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit").value("DAY"))
                .andExpect(jsonPath("$.data[0].amount").value(1000));
    }
}
