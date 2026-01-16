package com.aespa.armageddon.core.api.user.controller;

import com.aespa.armageddon.core.api.user.dto.request.UserUpdateRequest;
import com.aespa.armageddon.core.api.user.dto.response.UserProfileResponse;
import com.aespa.armageddon.core.api.user.dto.response.UserResponse;
import com.aespa.armageddon.core.common.support.response.ApiResult;
import com.aespa.armageddon.core.domain.auth.entity.User;
import com.aespa.armageddon.core.domain.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ApiResult<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getProfile(userDetails.getUsername());
        return ApiResult.success(UserProfileResponse.from(user));
    }

    @PutMapping("/update")
    @Operation(summary = "Update current user profile")
    public ApiResult<UserResponse> updateUser(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody UserUpdateRequest request) {
        User updated = userService.updateProfile(
                userDetails.getUsername(),
                request.getCurrentPassword(),
                request.getLoginId(),
                request.getEmail(),
                request.getNickname()
        );
        return ApiResult.success(UserResponse.from(updated));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete current user")
    public ApiResult<?> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteAccount(userDetails.getUsername());
        return ApiResult.success();
    }
}
