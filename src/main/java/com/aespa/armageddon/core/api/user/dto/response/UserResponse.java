package com.aespa.armageddon.core.api.user.dto.response;

import com.aespa.armageddon.core.domain.auth.entity.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private final Long id;
    private final String loginId;
    private final String email;
    private final String nickname;

    private UserResponse(Long id, String loginId, String email, String nickname) {
        this.id = id;
        this.loginId = loginId;
        this.email = email;
        this.nickname = nickname;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getLoginId(), user.getEmail(), user.getNickname());
    }
}
