package com.aespa.armageddon.core.domain.goal.repository;

import com.aespa.armageddon.core.domain.goal.domain.Goal;
import com.aespa.armageddon.core.domain.goal.domain.GoalStatus;
import com.aespa.armageddon.core.domain.goal.domain.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdAndStatusNot(Long userId, GoalStatus status);

    Optional<Goal> findByGoalIdAndUserId(Long goalId, Long userId);

    boolean existsByUserIdAndGoalTypeAndStatus(Long userId, GoalType goalType, GoalStatus status);

    boolean existsByUserIdAndGoalTypeAndExpenseCategoryAndStatus(Long userId, GoalType goalType,
            com.aespa.armageddon.core.domain.goal.domain.ExpenseCategory expenseCategory, GoalStatus status);
}
