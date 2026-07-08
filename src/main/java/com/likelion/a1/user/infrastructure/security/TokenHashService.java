package com.likelion.a1.user.infrastructure.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.security.MessageDigest;

//DB에 인증번호 hash로 암호화 저장
@Component
public class TokenHashService {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRefreshToken() {
      byte[] bytes = new byte[64];
      secureRandom.nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    public String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available", exception);
    }
  }
}
