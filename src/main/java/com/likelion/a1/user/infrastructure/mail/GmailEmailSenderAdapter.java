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

    applyCommonHeaders(message, to);
    message.setSubject(verificationSubjectOf(purpose));
    message.setText(verificationBodyOf(code));

    mailSender.send(message);
  }

  @Override
  public void sendSignupApproved(String to, String name) {
    SimpleMailMessage message = new SimpleMailMessage();

    applyCommonHeaders(message, to);
    message.setSubject("[AI Media Studio] 회원가입이 승인되었습니다.");
    message.setText(signupApprovedBodyOf(name));

    mailSender.send(message);
  }

  private void applyCommonHeaders(SimpleMailMessage message, String to) {
    if (StringUtils.hasText(fromEmail)) {
      message.setFrom(fromEmail);
    }

    message.setTo(to);
  }

  private String verificationSubjectOf(String purpose) {
    return switch (purpose) {
      case "SIGNUP" -> "[AI Media Studio] 회원가입 이메일 인증번호";
      case "PASSWORD_RESET" -> "[AI Media Studio] 비밀번호 재설정 인증번호";
      default -> "[AI Media Studio] 이메일 인증번호";
    };
  }

  private String verificationBodyOf(String code) {
    return """
        안녕하세요.
        AI Media Studio 이메일 인증번호입니다.

        인증번호: %s

        이 인증번호는 5분 동안만 유효합니다.
        본인이 요청하지 않았다면 이 메일을 무시해주세요.
        """
        .formatted(code);
  }

  private String signupApprovedBodyOf(String name) {
    String displayName = StringUtils.hasText(name) ? name.trim() : "회원";

    return """
        안녕하세요, %s님.

        AI Media Studio 회원가입이 승인되었습니다.
        이제 로그인 후 서비스를 이용하실 수 있습니다.

        본인이 요청하지 않은 가입이라면 관리자에게 문의해주세요.
        """
        .formatted(displayName);
  }
}
