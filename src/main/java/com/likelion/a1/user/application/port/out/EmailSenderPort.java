package com.likelion.a1.user.application.port.out;

public interface EmailSenderPort {
  void sendVerificationCode(String to, String purpose, String code);

  void sendSignupApproved(String to, String name);
}
