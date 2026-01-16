package com.aespa.armageddon.core.domain.transaction.command.application.controller;

import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionEditRequest;
import com.aespa.armageddon.core.domain.transaction.command.application.dto.request.TransactionWriteRequest;
import com.aespa.armageddon.core.domain.transaction.command.application.service.TransactionService;
import com.aespa.armageddon.infra.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transaction")
@Tag(name = "Transactions", description = "Transaction write endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/write")
    @Operation(summary = "Create transaction")
    public ApiResult<?> writeTransaction(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,
            @RequestBody TransactionWriteRequest request) {

        String token = authorization.substring(7);
        Long userNo = jwtTokenProvider.getUserIdFromJWT(token);
        transactionService.writeTransaction(userNo, request);
        return ApiResult.success();
    }

    @PutMapping("/edit/{transactionId}")
    @Operation(summary = "Edit transaction")
    public ApiResult<?> editTransaction(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Transaction id")
            @PathVariable Long transactionId,
            @RequestBody TransactionEditRequest request
    ) {
        String token = authorization.substring(7);
        Long userNo = jwtTokenProvider.getUserIdFromJWT(token);
        transactionService.editTransaction(userNo, transactionId, request);
        return ApiResult.success();
    }

    @DeleteMapping("/delete/{transactionId}")
    @Operation(summary = "Delete transaction")
    public ApiResult<?> deleteTransaction(
            @Parameter(description = "Bearer access token", required = true, example = "Bearer eyJ...")
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "Transaction id")
            @PathVariable Long transactionId
    ) {
        String token = authorization.substring(7);
        Long userNo = jwtTokenProvider.getUserIdFromJWT(token);

        transactionService.deleteTransaction(userNo, transactionId);
        return ApiResult.success();
    }
}
