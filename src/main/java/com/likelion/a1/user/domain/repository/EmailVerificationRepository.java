package com.likelion.a1.user.domain.repository;

import com.likelion.a1.user.domain.model.EmailVerification;
import java.util.Optional;

public interface EmailVerificationRepository {
    EmailVerification save(EmailVerification verification);
    //이메일 최신 인증번호 조회
    //purpose: 인증번호 사용 목적 (회원가입, 비밀번호 재설정 등)
    Optional<EmailVerification> findLatest(String email,String purpose);
}
