package com.aespa.armageddon.core.domain.transaction.query.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class TransactionSummaryResponse {

    private Long totalIncome;       // 총 수입
    private Long totalExpense;      // 총 지출
    private Long balance;           // 잔액 (총 수입 - 총 지출)

    @QueryProjection
    public TransactionSummaryResponse(Long totalIncome, Long totalExpense) {
        // null값이 넘어올 경우 0으로 처리
        this.totalIncome = totalIncome != null ? totalIncome : 0L;
        this.totalExpense = totalExpense != null ? totalExpense : 0L;
        this.balance = this.totalIncome - this.totalExpense;
    }
}
