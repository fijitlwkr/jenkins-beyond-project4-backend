package com.aespa.armageddon.core.domain.transaction.command.domain.aggregate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_transaction")
@NoArgsConstructor
@Getter
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private Long userNo;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 255)
    private String memo;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private Category category;

    public Transaction(
            Long userNo,
            String title,
            String memo,
            int amount,
            LocalDate date,
            TransactionType type,
            Category category
    ) {
        this.userNo = userNo;
        this.title = title;
        this.memo = memo;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.category = category;

        validate();
    }

    private void validate() {

        if (userNo == null) {
            throw new IllegalArgumentException("사용자 정보는 필수입니다.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }

        if (date == null) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }

        if (type == null) {
            throw new IllegalArgumentException("거래 타입은 필수입니다.");
        }

        if (type == TransactionType.EXPENSE && category == null) {
            throw new IllegalArgumentException("지출일 경우 카테고리는 필수입니다.");
        }

        if (type == TransactionType.INCOME && category != null) {
            throw new IllegalArgumentException("수입일 경우 카테고리를 입력할 수 없습니다.");
        }
    }

    public void edit(
            String title,
            String memo,
            int amount,
            LocalDate date,
            TransactionType type,
            Category category
    ) {
        this.title = title;
        this.memo = memo;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.category = category;

        validate();
    }

}

