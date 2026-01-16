package com.aespa.armageddon.core.domain.transaction.query.dto;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionLatelyResponse {

    private Long id;              // Transaction PK
    private LocalDate date;       // 거래 날짜
    private String title;         // 거래 제목
    private int amount;           // 거래 금액
    private Category category;    // 카테고리
    private TransactionType type; // 타입 (INCOME/EXPENSE)

    @QueryProjection
    public TransactionLatelyResponse(Long id, LocalDate date, String title, int amount,Category category, TransactionType type) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
    }
}
