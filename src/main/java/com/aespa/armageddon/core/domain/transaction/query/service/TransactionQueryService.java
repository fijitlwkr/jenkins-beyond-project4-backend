package com.aespa.armageddon.core.domain.transaction.query.service;

import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionDailyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionLatelyResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionResponse;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionSummaryResponse;
import com.aespa.armageddon.core.domain.transaction.query.repository.TransactionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용 (성능 최적화 & 데이터 변경 실수 방지)
public class TransactionQueryService {

    private final TransactionQueryRepository transactionQueryRepository;

    /*
     * 최근 거래 내역 리스트 조회
     */
    public List<TransactionLatelyResponse> getLatelyTransactions(
            Long userNo) {

        return transactionQueryRepository.findLatelyList(userNo);

    }

    /*
     * 일간 가계부 내역 조회
     * 요청을 받으면 Repository로 전달
     */
    public List<TransactionDailyResponse> getDailyTransactions(Long userNo, LocalDate date) {

        return transactionQueryRepository.findDailyList(userNo, date);

    }

    /* 지출,수입 입력/수정 모달창 */
    public List<TransactionResponse> getTransactions(Long id, Long transactionId) {

        return transactionQueryRepository.findTransaction(id, transactionId);

    }

    /**
     * 일간 요약 정보 조회 (수입, 지출, 잔액)
     */
    public TransactionSummaryResponse getDailySummary(Long userNo, LocalDate date) {

        return transactionQueryRepository.findDailySummary(userNo, date);

    }

    /**
     * 월간 요약 정보 조회 (수입, 지출, 잔액)
     */
    public TransactionSummaryResponse getMonthlySummary(Long userNo, int year, int month) {

        return transactionQueryRepository.findMonthlySummary(userNo, year, month);

    }

    public Long getTransactionSum(Long userNo,
            com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category category,
            com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType type,
            LocalDate startDate, LocalDate endDate) {
        return transactionQueryRepository.findSum(userNo, category, type, startDate, endDate);
    }

}
