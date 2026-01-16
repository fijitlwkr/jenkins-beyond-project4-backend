package com.aespa.armageddon.core.domain.goal.infra;

import com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory;
import com.aespa.armageddon.core.domain.goal.domain.GoalType;
import com.aespa.armageddon.core.domain.goal.port.TransactionPort;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.core.domain.transaction.query.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionPort {

    private final TransactionQueryService transactionQueryService;

    @Override
    public long getTransactionSum(Long userId, GoalType goalType, ExpenseCategory expenseCategory, LocalDate startDate,
            LocalDate endDate) {
        Category category = (goalType == GoalType.SAVING)
                ? Category.SAVING
                : Category.valueOf(expenseCategory.name());

        return transactionQueryService.getTransactionSum(
                userId,
                category,
                TransactionType.EXPENSE,
                startDate,
                endDate);
    }
}
