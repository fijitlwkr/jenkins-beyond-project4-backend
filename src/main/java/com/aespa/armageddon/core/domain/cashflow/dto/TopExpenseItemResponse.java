package com.aespa.armageddon.core.domain.cashflow.dto;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TopExpenseItemResponse {

    private Long transactionId;
    private String title;          // 거래명
    private int amount;           // 지출 금액
    private Category category;     // 카테고리
    private LocalDate date;         // 지출 일자
}