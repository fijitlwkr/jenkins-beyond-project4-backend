package com.aespa.armageddon.core.domain.transaction.query.service;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionDailyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionLatelyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionSummaryResponse;
import com.aespa.armageddon.core.domain.transaction.query.repository.TransactionQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

        @InjectMocks
        private TransactionQueryService transactionQueryService;

        @Mock
        private TransactionQueryRepository transactionQueryRepository;

        @Test
        @DisplayName("최근 거래 내역 조회")
        void getLatelyTransactions() {
                // given
                Long userNo = 1L;
                // Constructor: id, date, title, amount, category, type
                TransactionLatelyResponse response = new TransactionLatelyResponse(1L, LocalDate.now(), "title", 1000,
                                Category.FOOD, TransactionType.EXPENSE);
                given(transactionQueryRepository.findLatelyList(userNo))
                                .willReturn(List.of(response));

                // when
                List<TransactionLatelyResponse> result = transactionQueryService.getLatelyTransactions(userNo);

                // then
                assertThat(result).hasSize(1);
                verify(transactionQueryRepository).findLatelyList(userNo);
        }

        @Test
        @DisplayName("일간 상세 내역 조회")
        void getDailyTransactions() {
                // given
                Long userNo = 1L;
                LocalDate date = LocalDate.now();
                // Constructor: id, type, title, amount, category
                TransactionDailyResponse response = new TransactionDailyResponse(1L, TransactionType.EXPENSE, "title",
                                1000, Category.FOOD);

                given(transactionQueryRepository.findDailyList(userNo, date))
                                .willReturn(List.of(response));

                // when
                List<TransactionDailyResponse> result = transactionQueryService.getDailyTransactions(userNo, date);

                // then
                assertThat(result).hasSize(1);
                verify(transactionQueryRepository).findDailyList(userNo, date);
        }

        @Test
        @DisplayName("일간 요약 정보 조회")
        void getDailySummary() {
                // given
                Long userNo = 1L;
                LocalDate date = LocalDate.now();
                // Constructor: income, expense (balance calculated internally)
                TransactionSummaryResponse response = new TransactionSummaryResponse(1000L, 500L);

                given(transactionQueryRepository.findDailySummary(userNo, date))
                                .willReturn(response);

                // when
                TransactionSummaryResponse result = transactionQueryService.getDailySummary(userNo, date);

                // then
                assertThat(result.getBalance()).isEqualTo(500L);
                verify(transactionQueryRepository).findDailySummary(userNo, date);
        }

        @Test
        @DisplayName("월간 요약 정보 조회")
        void getMonthlySummary() {
                // given
                Long userNo = 1L;
                int year = 2024;
                int month = 5;
                TransactionSummaryResponse response = new TransactionSummaryResponse(1000L, 500L);

                given(transactionQueryRepository.findMonthlySummary(userNo, year, month))
                                .willReturn(response);

                // when
                TransactionSummaryResponse result = transactionQueryService.getMonthlySummary(userNo, year, month);

                // then
                assertThat(result.getTotalIncome()).isEqualTo(1000L);
                verify(transactionQueryRepository).findMonthlySummary(userNo, year, month);
        }
}
