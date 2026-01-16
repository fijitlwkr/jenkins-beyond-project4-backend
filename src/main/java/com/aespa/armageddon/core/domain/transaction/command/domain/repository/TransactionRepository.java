package com.aespa.armageddon.core.domain.transaction.command.domain.repository;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Transaction;

import java.util.Optional;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    void delete(Transaction transaction);
}
