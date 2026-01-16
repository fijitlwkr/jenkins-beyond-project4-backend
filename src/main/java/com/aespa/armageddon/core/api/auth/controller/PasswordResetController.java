package com.aespa.armageddon.core.api.auth.controller;

import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetConfirmRequest;
import com.aespa.armageddon.core.api.auth.dto.request.PasswordResetRequest;
import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password/reset")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Password reset endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    @Operation(summary = "Request password reset")
    public ApiResult<?> requestReset(@Valid @RequestBody PasswordResetRequest req) {
        passwordResetService.requestReset(req);
        return ApiResult.success();
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm password reset")
    public ApiResult<?> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordResetService.confirmReset(req);
        return ApiResult.success();
    }
}
