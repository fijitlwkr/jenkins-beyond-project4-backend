package com.aespa.armageddon.core.api.auth.controller;

import com.aespa.armageddon.core.api.auth.dto.request.LoginRequest;
import com.aespa.armageddon.core.api.auth.dto.request.RefreshRequest;
import com.aespa.armageddon.core.api.auth.dto.request.SignupRequest;
import com.aespa.armageddon.core.api.auth.dto.response.TokenResponse;
import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.auth.service.AuthService;
import com.aespa.armageddon.core.domain.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up")
    public ApiResult<Long> signup(@RequestBody @Valid SignupRequest request) {
        Long userId = userService.signup(request);
        return ApiResult.success(userId);
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ApiResult<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ApiResult.success(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResult<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResult.success(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ApiResult<?> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResult.success();
    }
}
