package com.aespa.armageddon.core.domain.cashflow.repository;

import com.aespa.armageddon.core.domain.cashflow.dto.*;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatisticsRepositoryImpl implements StatisticsRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public IncomeExpenseSum findIncomeExpenseSum(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Object[] result = (Object[]) em.createQuery("""
            SELECT
              COALESCE(SUM(CASE WHEN t.type = :income THEN t.amount ELSE 0 END), 0),
              COALESCE(SUM(CASE WHEN t.type = :expense THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.userNo = :userNo
            AND t.date BETWEEN :start AND :end
        """)
                .setParameter("income", TransactionType.INCOME)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("userNo", userNo)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getSingleResult();

        return new IncomeExpenseSum(
                ((Number) result[0]).longValue(),
                ((Number) result[1]).longValue()
        );
    }

    @Override
    public List<CategoryExpenseSum> findCategoryExpenseSum(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return em.createQuery("""
        SELECT new com.aespa.armageddon.core.domain.cashflow.dto.CategoryExpenseSum(
            t.category,
            SUM(t.amount)
        )
        FROM Transaction t
        WHERE t.userNo = :userNo
          AND t.type = :expense
          AND t.date BETWEEN :start AND :end
        GROUP BY t.category
    """, CategoryExpenseSum.class)
                .setParameter("userNo", userNo)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getResultList();
    }

    @Override
    public List<TopExpenseItemResponse> findTopExpenseItems(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            int limit
    ) {
        return em.createQuery("""
        SELECT new com.aespa.armageddon.core.domain.cashflow.dto.TopExpenseItemResponse(
            t.transactionId,
            t.title,
            t.amount,
            t.category,
            t.date
        )
        FROM Transaction t
        WHERE t.userNo = :userNo
          AND t.type = :expense
          AND t.date BETWEEN :start AND :end
        ORDER BY t.amount DESC
    """, TopExpenseItemResponse.class)
                .setParameter("userNo", userNo)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .setMaxResults(limit)
                .getResultList();
    }

    //추이통계 파트
    @Override
    public List<ExpenseTrendRawDto> findExpenseTrend(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate,
            TrendUnit unit
    ) {
        return switch (unit) {
            case DAY -> findByDay(userNo, startDate, endDate);
            case WEEK -> findByWeek(userNo, startDate, endDate);
            case MONTH -> findByMonth(userNo, startDate, endDate);
        };
    }

    private List<ExpenseTrendRawDto> findByDay(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return em.createQuery("""
        SELECT new com.aespa.armageddon.core.domain.cashflow.dto.ExpenseTrendRawDto(
            t.date,
            SUM(t.amount)
        )
        FROM Transaction t
        WHERE t.userNo = :userNo
          AND t.type = :expense
          AND t.date BETWEEN :start AND :end
        GROUP BY t.date
        ORDER BY t.date
    """, ExpenseTrendRawDto.class)
                .setParameter("userNo", userNo)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getResultList();
    }


    private List<ExpenseTrendRawDto> findByWeek(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return em.createQuery("""
        SELECT new com.aespa.armageddon.core.domain.cashflow.dto.ExpenseTrendRawDto(
            t.date,
            SUM(t.amount)
        )
        FROM Transaction t
        WHERE t.userNo = :userNo
          AND t.type = :expense
          AND t.date BETWEEN :start AND :end
        GROUP BY YEAR(t.date), WEEK(t.date)
        ORDER BY YEAR(t.date), WEEK(t.date)
    """, ExpenseTrendRawDto.class)
                .setParameter("userNo", userNo)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getResultList();
    }


    private List<ExpenseTrendRawDto> findByMonth(
            Long userNo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return em.createQuery("""
        SELECT new com.aespa.armageddon.core.domain.cashflow.dto.ExpenseTrendRawDto(
            t.date,
            SUM(t.amount)
        )
        FROM Transaction t
        WHERE t.userNo = :userNo
          AND t.type = :expense
          AND t.date BETWEEN :start AND :end
        GROUP BY YEAR(t.date), MONTH(t.date)
        ORDER BY YEAR(t.date), MONTH(t.date)
    """, ExpenseTrendRawDto.class)
                .setParameter("userNo", userNo)
                .setParameter("expense", TransactionType.EXPENSE)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getResultList();
    }




}
