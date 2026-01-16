package com.aespa.armageddon.core.domain.transaction.query.dto;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class TransactionDailyResponse {

    private Long id;                // Transaction PK
    private TransactionType type;   // 타입 (INCOME/EXPENSE)
    private String title;           // 거래 제목
    private int amount;             // 거래 금액
    private Category category;      // 카테고리

    @QueryProjection
    public TransactionDailyResponse(Long id, TransactionType type, String title, int amount, Category category) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.category = category;
    }
}
