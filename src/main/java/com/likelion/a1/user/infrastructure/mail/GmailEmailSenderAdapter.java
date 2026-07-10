package com.likelion.a1.user.infrastructure.mail;

import com.likelion.a1.user.application.port.out.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GmailEmailSenderAdapter implements EmailSenderPort {
  private final JavaMailSender mailSender;
  private final String fromEmail;

  public GmailEmailSenderAdapter(
      JavaMailSender mailSender, @Value("${app.mail.from:}") String fromEmail) {
    this.mailSender = mailSender;
    this.fromEmail = fromEmail;
  }

  @Override
  public void sendVerificationCode(String to, String purpose, String code) {
    SimpleMailMessage message = new SimpleMailMessage();

    if (StringUtils.hasText(fromEmail)) {
      message.setFrom(fromEmail);
    }

    message.setTo(to);
    message.setSubject(subjectOf(purpose));
    message.setText(bodyOf(code));

    mailSender.send(message);
  }

  private String subjectOf(String purpose) {
    return switch (purpose) {
      case "SIGNUP" -> "[AI Media Studio] 회원가입 이메일 인증번호";
      case "PASSWORD_RESET" -> "[AI Media Studio] 비밀번호 재설정 인증번호";
      default -> "[AI Media Studio] 이메일 인증번호";
    };
  }

  private String bodyOf(String code) {
    return """
        안녕하세요.
        AI Media Studio 이메일 인증번호입니다.

        인증번호: %s

        이 인증번호는 5분 동안만 유효합니다.
        본인이 요청하지 않았다면 이 메일을 무시해주세요.
        """
        .formatted(code);
  }
}
