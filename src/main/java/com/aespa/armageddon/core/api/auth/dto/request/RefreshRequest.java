package com.aespa.armageddon.core.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required.")
    private String refreshToken;
}
