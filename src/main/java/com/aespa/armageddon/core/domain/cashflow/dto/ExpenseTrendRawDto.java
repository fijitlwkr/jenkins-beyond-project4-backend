package com.aespa.armageddon.core.domain.cashflow.dto;


import java.time.LocalDate;

public record ExpenseTrendRawDto(
        LocalDate date,
        Long amount
) {}