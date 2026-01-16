package com.aespa.armageddon.core.api.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Login ID is required.")
    @Size(min = 4, max = 20, message = "Login ID must be between 4 and 20 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Login ID must contain only letters and numbers.")
    private String loginId;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email format is invalid.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters.")
    private String password;

    @NotBlank(message = "Nickname is required.")
    @Size(min = 2, max = 10, message = "Nickname must be between 2 and 10 characters.")
    private String nickname;
}
