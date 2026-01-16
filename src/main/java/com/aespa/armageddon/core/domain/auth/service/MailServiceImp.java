package com.aespa.armageddon.core.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailServiceImp implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    private String getFromAddress() {
        return mailProperties.getUsername();
    }

    private void sendHtmlMail(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String html = templateEngine.process(templateName, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                            StandardCharsets.UTF_8.name());

            helper.setFrom(getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email.", e);
        }
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        String subject = "[armageddon] Password reset code";

        Map<String, Object> vars = new HashMap<>();
        vars.put("code", code);

        sendHtmlMail(to, subject, "mail/ResetPassword", vars);
    }

    @Override
    public void sendEmailVerificationCode(String to, String code) {
        String subject = "[armageddon] Email verification code";

        Map<String, Object> vars = new HashMap<>();
        vars.put("code", code);

        sendHtmlMail(to, subject, "mail/VerifyEmail", vars);
    }
}
