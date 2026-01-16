package com.aespa.armageddon.core.domain.transaction.command.infrastructure.repository;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Transaction;
import com.aespa.armageddon.core.domain.transaction.command.domain.repository.TransactionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTransactionRepository extends TransactionRepository, JpaRepository<Transaction, Long> {
}
