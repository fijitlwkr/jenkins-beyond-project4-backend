package com.aespa.armageddon.core.domain.transaction.query.dto;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionResponse {

    private Long id;                // Transaction 의 PK(수정/삭제용)
    private TransactionType type;   // INCOME(수입) / EXPENDITURE(지출)
    private LocalDate date;         // 거래 날짜
    private String title;           // 거래 제목
    private int amount;             // 거래 금액
    private Category category;      // 카테고리
    private String memo;            // 거래 메모

    @QueryProjection
    public TransactionResponse(Long id,TransactionType type,LocalDate date, String title, int amount, Category category, String memo) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.memo = memo;
    }
}
