package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// purpose: 인증번호 사용 목적 (회원가입, 비밀번호 재설정 등)
//최신 인증번호 가져옴
interface SpringDataEmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(
      String email,
      String purpose
  );
    
}
