package com.aespa.armageddon.core.domain.transaction.query.repository;

import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.core.domain.transaction.query.dto.*;
import com.aespa.armageddon.core.domain.transaction.query.dto.TransactionLatelyResponse;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.QTransaction.transaction;

@Repository
@RequiredArgsConstructor
public class TransactionQueryRepository {

        private final JPAQueryFactory queryFactory; // QueryDSL을 사용하기 위한 JPAQueryFactory 주입

        /* 최근 거래 내역 리스트 조회 */
        public List<TransactionLatelyResponse> findLatelyList(Long userNo) {
                return queryFactory
                                .select(new QTransactionLatelyResponse(
                                                transaction.transactionId,
                                                transaction.date,
                                                transaction.title,
                                                transaction.amount,
                                                transaction.category,
                                                transaction.type))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo) // 유저 본인 내역 조회
                                )
                                .orderBy(transaction.date.desc()) // 날짜 내림차순
                                .limit(5) // 5개만 조회
                                .fetch();
        }

        /* 특정 날짜별 거래 내역 조회 */
        public List<TransactionDailyResponse> findDailyList(Long userNo, LocalDate date) {
                return queryFactory
                                .select(new QTransactionDailyResponse(
                                                transaction.transactionId,
                                                transaction.type,
                                                transaction.title,
                                                transaction.amount,
                                                transaction.category))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo), // 유저 본인 내역 조회
                                                transaction.date.eq(date) // 요청한 날짜의 내역 조회
                                )
                                .fetch();
        }

        /* 수입, 지출 입력/수정 모달창 */
        public List<TransactionResponse> findTransaction(Long userNo, Long transactionId) {
                return queryFactory
                                .select(new QTransactionResponse(
                                                transaction.transactionId,
                                                transaction.type,
                                                transaction.date,
                                                transaction.title,
                                                transaction.amount,
                                                transaction.category,
                                                transaction.memo))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo), // 유저 본인 내역 조회
                                                transaction.transactionId.eq(transactionId) // 요청한 날짜의 내역 조회
                                )
                                .fetch();

        }


        /* 월간 총 수입/지출/잔액 요약 조회 */
        public TransactionSummaryResponse findMonthlySummary(Long userNo, int year, int month) {

                // 검색할 월의 시작일과 마지막일 계산
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                return queryFactory
                                .select(new QTransactionSummaryResponse(
                                                // 1. 수입 합계
                                                new CaseBuilder()
                                                                .when(transaction.type.eq(TransactionType.INCOME))
                                                                .then(transaction.amount.longValue()) // int -> long
                                                                                                      // 타입변환
                                                                .otherwise(0L) // 조건 맞지 않으면 0
                                                                .sum(), // 합계

                                                // 2. 지출 합계
                                                new CaseBuilder()
                                                                .when(transaction.type.eq(TransactionType.EXPENSE))
                                                                .then(transaction.amount.longValue())
                                                                .otherwise(0L)
                                                                .sum()))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo),
                                                transaction.date.between(startDate, endDate) // 해당 월 데이터만
                                )
                                .fetchOne();
        }

        /* 일간 총 수입/지출/잔액 요약 조회 */
        public TransactionSummaryResponse findDailySummary(Long userNo, LocalDate date) {
                return queryFactory
                                .select(new QTransactionSummaryResponse(
                                                // 1. 수입 합계
                                                new CaseBuilder()
                                                                .when(transaction.type.eq(TransactionType.INCOME))
                                                                .then(transaction.amount.longValue())
                                                                .otherwise(0L)
                                                                .sum(),

                                                // 2. 지출 합계
                                                new CaseBuilder()
                                                                .when(transaction.type.eq(TransactionType.EXPENSE))
                                                                .then(transaction.amount.longValue())
                                                                .otherwise(0L)
                                                                .sum()))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo),
                                                transaction.date.eq(date) // 해당 날짜 데이터만
                                )
                                .fetchOne();
        }

        public Long findSum(Long userNo, Category category, TransactionType type, LocalDate startDate,
                        LocalDate endDate) {
                return queryFactory
                                .select(transaction.amount.sum().coalesce(0))
                                .from(transaction)
                                .where(
                                                transaction.userNo.eq(userNo),
                                                transaction.type.eq(type),
                                                category != null ? transaction.category.eq(category) : null,
                                                transaction.date.between(startDate, endDate))
                                .fetchOne()
                                .longValue();
        }
}
