package com.aespa.armageddon.core.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Login ID is required.")
    private String loginId;

    @NotBlank(message = "Password is required.")
    private String password;
}
