package com.aespa.armageddon.core.domain.goal.port;

import com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory;
import com.aespa.armageddon.core.domain.goal.domain.GoalType;

import java.time.LocalDate;

public interface TransactionPort {
    long getTransactionSum(Long userId, GoalType goalType, ExpenseCategory expenseCategory, LocalDate startDate,
            LocalDate endDate);
}
