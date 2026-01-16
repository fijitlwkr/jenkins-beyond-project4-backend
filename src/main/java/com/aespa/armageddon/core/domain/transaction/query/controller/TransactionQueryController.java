package com.aespa.armageddon.core.domain.transaction.query.controller;

import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.repository.UserRepository;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionDailyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionLatelyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionSummaryResponse;
import com.aespa.armageddon.core.domain.transaction.query.service.TransactionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
@Tag(name = "Transactions", description = "Transaction query endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionQueryController {

        private final TransactionQueryService transactionQueryService;
        private final UserRepository userRepository;

        /* 최근 거래 내역 리스트 조회 */
        @GetMapping("/list")
        @Operation(summary = "Get top 5 recent transactions")
        public ApiResult<List<TransactionLatelyResponse>> getLatelyTransactions(
                        @AuthenticationPrincipal UserDetails userDetails) {

                User user = userRepository.findByLoginId(userDetails.getUsername())
                                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

                return ApiResult.success(transactionQueryService.getLatelyTransactions(user.getId()));
        }

        /* 일간 상세 내역 조회 (날짜 클릭 시 리스트) */
        @GetMapping("/daily")
        @Operation(summary = "Get daily transactions")
        public ApiResult<List<TransactionDailyResponse>> getDailyTransactions(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Parameter(description = "Date to query (YYYY-MM-DD)")
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

                User user = userRepository.findByLoginId(userDetails.getUsername())
                                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

                return ApiResult.success(transactionQueryService.getDailyTransactions(user.getId(), date));
        }

        /* 수입, 지출 입력/수정 모달창*/
        @GetMapping("/modal")
        @Operation(summary = "Get transaction detail")
        public ApiResult<List<TransactionResponse>> getTransactions(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Parameter(description = "Transaction ID")
                        @RequestParam Long transactionId
                        ) {

                User user = userRepository.findByLoginId(userDetails.getUsername())
                        .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

                return ApiResult.success(transactionQueryService.getTransactions(user.getId(), transactionId));
        }

        /* 일간 총 수입/지출/잔액 요약 조회 */
        @GetMapping("/daily/summary")
        @Operation(summary = "Get daily transaction summary(income, expense, balance)")
        public ApiResult<TransactionSummaryResponse> getDailySummary(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Parameter(description = "Date to query (YYYY-MM-DD)")
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

                User user = userRepository.findByLoginId(userDetails.getUsername())
                                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

                return ApiResult.success(transactionQueryService.getDailySummary(user.getId(), date));
        }

        /* 월간 요약 정보 조회 (수입, 지출, 잔액) */
        @GetMapping("/monthly")
        @Operation(summary = "Get monthly transaction summary")
        public ApiResult<TransactionSummaryResponse> getMonthlySummary(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @Parameter(description = "Year (e.g. 2025)") @RequestParam int year,
                        @Parameter(description = "Month (1-12)") @RequestParam int month) {

                User user = userRepository.findByLoginId(userDetails.getUsername())
                                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));

                return ApiResult.success(transactionQueryService.getMonthlySummary(user.getId(), year, month));
        }
}
