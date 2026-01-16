package com.aespa.armageddon.core.domain.cashflow.dto;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;

public record CategoryExpenseRatio(
        Category category,
        long totalExpense,
        double ratio   // %
) {
}
