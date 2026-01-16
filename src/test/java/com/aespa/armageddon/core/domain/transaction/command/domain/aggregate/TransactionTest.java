package com.aespa.armageddon.core.domain.transaction.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    @DisplayName("거래 생성 성공 - 지출")
    void create_Expense_Success() {
        Transaction tx = new Transaction(
                1L, "점심", "메모", 5000,
                LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        assertThat(tx).isNotNull();
        assertThat(tx.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("거래 생성 성공 - 수입")
    void create_Income_Success() {
        Transaction tx = new Transaction(
                1L, "월급", "메모", 3000000,
                LocalDate.now(), TransactionType.INCOME, null);

        assertThat(tx).isNotNull();
        assertThat(tx.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    @DisplayName("생성 실패 - 필수값 누락 (UserNo)")
    void create_Fail_UserNoNull() {
        assertThatThrownBy(() -> new Transaction(
                null, "제목", "메모", 1000,
                LocalDate.now(), TransactionType.EXPENSE, Category.FOOD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 정보는 필수입니다.");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    @DisplayName("생성 실패 - 제목 누락")
    void create_Fail_TitleEmpty(String title) {
        assertThatThrownBy(() -> new Transaction(
                1L, title, "메모", 1000,
                LocalDate.now(), TransactionType.EXPENSE, Category.FOOD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("제목은 필수입니다.");
    }

    @Test
    @DisplayName("생성 실패 - 금액 0 이하")
    void create_Fail_AmountZero() {
        assertThatThrownBy(() -> new Transaction(
                1L, "제목", "메모", 0,
                LocalDate.now(), TransactionType.EXPENSE, Category.FOOD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("생성 실패 - 지출인데 카테고리 없음")
    void create_Fail_ExpenseNoCategory() {
        assertThatThrownBy(() -> new Transaction(
                1L, "제목", "메모", 1000,
                LocalDate.now(), TransactionType.EXPENSE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지출일 경우 카테고리는 필수입니다.");
    }

    @Test
    @DisplayName("생성 실패 - 수입인데 카테고리 있음")
    void create_Fail_IncomeWithCategory() {
        assertThatThrownBy(() -> new Transaction(
                1L, "제목", "메모", 1000,
                LocalDate.now(), TransactionType.INCOME, Category.FOOD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수입일 경우 카테고리를 입력할 수 없습니다.");
    }
}
