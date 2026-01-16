package com.aespa.armageddon.core.api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequest {
    @NotBlank(message = "Email is required.")
    @Email(message = "Email format is invalid.")
    private String email;
}
