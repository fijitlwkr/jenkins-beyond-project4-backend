package com.aespa.armageddon.core.domain.auth.service;

public interface MailService {
    void sendPasswordResetCode(String to, String code);

    void sendEmailVerificationCode(String to, String code);
}
