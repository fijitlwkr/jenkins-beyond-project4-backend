package com.aespa.armageddon.core.domain.goal.controller;

import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateExpenseGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.CreateSavingGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.request.UpdateGoalRequest;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalDetailResponse;
import com.aespa.armageddon.core.domain.goal.dto.response.GoalResponse;
import com.aespa.armageddon.core.domain.goal.service.GoalService;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Goal management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class GoalController {

        private final GoalService goalService;
        private final JwtTokenProvider jwtTokenProvider;

        private Long extractUserId(String authorization) {
                String token = authorization.substring(7); // "Bearer "
                return jwtTokenProvider.getUserIdFromJWT(token);
        }

        /**
         * 목표 전체 조회 (저축 + 지출)
         */
        @GetMapping
        @Operation(summary = "Get all goals")
        public ApiResult<List<GoalResponse>> getGoals(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization) {

                Long userId = extractUserId(authorization);
                return ApiResult.success(goalService.getGoals(userId));
        }

        /**
         * 목표 세부정보 조회 (진행률, 예측 포함)
         */
        @GetMapping("/{goalId}")
        @Operation(summary = "Get goal details")
        public ApiResult<GoalDetailResponse> getGoalDetail(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization,
                        @Parameter(description = "Goal id")
                        @PathVariable Long goalId) {

                Long userId = extractUserId(authorization);
                return ApiResult.success(goalService.getGoalDetail(userId, goalId));
        }

        /**
         * 저축 목표 생성
         */
        @PostMapping("/saving")
        @Operation(summary = "Create saving goal")
        public ApiResult<?> createSavingGoal(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization,
                        @RequestBody CreateSavingGoalRequest request) {

                Long userId = extractUserId(authorization);
                goalService.createSavingGoal(userId, request);
                return ApiResult.success();
        }

        /**
         * 지출 목표 생성
         */
        @PostMapping("/expense")
        @Operation(summary = "Create expense goal")
        public ApiResult<?> createExpenseGoal(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization,
                        @RequestBody CreateExpenseGoalRequest request) {

                Long userId = extractUserId(authorization);
                goalService.createExpenseGoal(userId, request);
                return ApiResult.success();
        }

        /**
         * 목표 수정 (금액 / 기간)
         */
        @PutMapping("/{goalId}")
        @Operation(summary = "Update goal")
        public ApiResult<?> updateGoal(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization,
                        @Parameter(description = "Goal id")
                        @PathVariable Long goalId,
                        @RequestBody UpdateGoalRequest request) {

                Long userId = extractUserId(authorization);
                goalService.updateGoal(userId, goalId, request);
                return ApiResult.success();
        }

        /**
         * 목표 삭제 (Soft Delete)
         */
        @DeleteMapping("/{goalId}")
        @Operation(summary = "Delete goal")
        public ApiResult<?> deleteGoal(
                        @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
                        @RequestHeader("Authorization") String authorization,
                        @Parameter(description = "Goal id")
                        @PathVariable Long goalId) {

                Long userId = extractUserId(authorization);
                goalService.deleteGoal(userId, goalId);
                return ApiResult.success();
        }
}
