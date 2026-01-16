package com.aespa.armageddon.core.domain.transaction.command.application.dto.request;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;

import java.time.LocalDate;

public record TransactionEditRequest(
        String title,
        String memo,
        int amount,
        LocalDate date,
        TransactionType type,
        Category category
) {

}
