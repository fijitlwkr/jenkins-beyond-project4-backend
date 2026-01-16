package com.aespa.armageddon.core.domain.transaction.command.application.service;

import com.aespa.armageddon.core.common.support.error.CoreException;
import com.aespa.armageddon.core.common.support.error.ErrorType;
import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionEditRequest;
import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionWriteRequest;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Category;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.Transaction;
import com.aespa.armageddon.core.domain.transaction.command.domain.aggregate.TransactionType;
import com.aespa.armageddon.core.domain.transaction.command.domain.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("거래 내역 작성 성공")
    void writeTransaction_Success() {
        // given
        Long userNo = 1L;
        TransactionWriteRequest request = new TransactionWriteRequest(
                "점심 식사",
                "편의점",
                5000,
                LocalDate.now(),
                TransactionType.EXPENSE,
                Category.FOOD);

        // when
        transactionService.writeTransaction(userNo, request);

        // then
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("거래 내역 수정 성공")
    void editTransaction_Success() {
        // given
        Long userNo = 1L;
        Long transactionId = 100L;
        TransactionEditRequest request = new TransactionEditRequest(
                "저녁 식사",
                "식당",
                12000,
                LocalDate.now(),
                TransactionType.EXPENSE,
                Category.FOOD);

        Transaction mockTransaction = new Transaction(
                userNo, "점심", "편의점", 5000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.of(mockTransaction));

        // when
        transactionService.editTransaction(userNo, transactionId, request);

        // then
        // 별도의 save 호출 없이 Dirty Checking으로 업데이트 되지만, 로직 실행 확인
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    @DisplayName("거래 내역 수정 실패 - 존재하지 않는 내역")
    void editTransaction_Fail_NotFound() {
        // given
        Long userNo = 1L;
        Long transactionId = 999L;
        TransactionEditRequest request = new TransactionEditRequest(
                "제목", "메모", 1000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transactionService.editTransaction(userNo, transactionId, request))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.TRANSACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("거래 내역 수정 실패 - 권한 없음")
    void editTransaction_Fail_AccessDenied() {
        // given
        Long ownerId = 1L;
        Long otherUserId = 2L;
        Long transactionId = 100L;
        TransactionEditRequest request = new TransactionEditRequest(
                "제목", "메모", 1000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        Transaction mockTransaction = new Transaction(
                ownerId, "점심", "편의점", 5000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.of(mockTransaction));

        // when & then
        assertThatThrownBy(() -> transactionService.editTransaction(otherUserId, transactionId, request))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("거래 내역 삭제 성공")
    void deleteTransaction_Success() {
        // given
        Long userNo = 1L;
        Long transactionId = 100L;

        Transaction mockTransaction = new Transaction(
                userNo, "점심", "편의점", 5000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.of(mockTransaction));

        // when
        transactionService.deleteTransaction(userNo, transactionId);

        // then
        verify(transactionRepository, times(1)).delete(mockTransaction);
    }

    @Test
    @DisplayName("거래 내역 삭제 실패 - 존재하지 않는 내역")
    void deleteTransaction_Fail_NotFound() {
        // given
        Long userNo = 1L;
        Long transactionId = 999L;

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> transactionService.deleteTransaction(userNo, transactionId))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.TRANSACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("거래 내역 삭제 실패 - 권한 없음")
    void deleteTransaction_Fail_AccessDenied() {
        // given
        Long ownerId = 1L;
        Long otherUserId = 2L;
        Long transactionId = 100L;

        Transaction mockTransaction = new Transaction(
                ownerId, "점심", "편의점", 5000, LocalDate.now(), TransactionType.EXPENSE, Category.FOOD);

        given(transactionRepository.findById(transactionId))
                .willReturn(Optional.of(mockTransaction));

        // when & then
        assertThatThrownBy(() -> transactionService.deleteTransaction(otherUserId, transactionId))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.ACCESS_DENIED);
    }
}