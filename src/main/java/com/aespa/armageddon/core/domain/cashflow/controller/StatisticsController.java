package com.aespa.armageddon.core.domain.cashflow.controller;

import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.cashflow.service.StatisticsService;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Cashflow statistics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/summary")
    @Operation(summary = "Get summary statistics")
    public ResponseEntity<SummaryStatisticsResponse> getSummaryStatistics(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        Long userNo = extractUserNo(authorization);

        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        return ResponseEntity.ok(
                statisticsService.getSummary(userNo, startDate, endDate)
        );
    }

    @GetMapping("/expense/categories")
    @Operation(summary = "Get expense ratio by category")
    public ResponseEntity<List<CategoryExpenseRatio>> getCategoryExpenseStatistics(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        Long userNo = extractUserNo(authorization);

        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        return ResponseEntity.ok(
                statisticsService.getCategoryExpenseWithRatio(
                        userNo, startDate, endDate
                )
        );
    }

    /**
     * 상위 지출 항목 조회
     */
    @GetMapping("/expense/top")
    @Operation(summary = "Get top expense items")
    public ResponseEntity<List<TopExpenseItemResponse>> getTopExpenseItems(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,

            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @Parameter(description = "Max number of items to return")
            @RequestParam(required = false)
            Integer limit
    ) {
        Long userNo = extractUserNo(authorization);

        return ResponseEntity.ok(
                statisticsService.getTopExpenseItems(
                        userNo,
                        startDate,
                        endDate,
                        limit
                )
        );
    }

    @GetMapping("/expense/trend")
    @Operation(summary = "Get expense trend")
    public ResponseEntity<ExpenseTrendResponse> getExpenseTrend(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,

            @Parameter(description = "Trend unit (e.g. DAILY, WEEKLY, MONTHLY)")
            @RequestParam TrendUnit unit,

            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        Long userNo = extractUserNo(authorization);

        // 기본값: 이번 달
        if (startDate == null || endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        return ResponseEntity.ok(
                statisticsService.getExpenseTrend(
                        userNo,
                        startDate,
                        endDate,
                        unit
                )
        );
    }

    //공통 메서드
    private Long extractUserNo(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }

        String token = authorization.substring(7);
        return jwtTokenProvider.getUserIdFromJWT(token);
    }


}
