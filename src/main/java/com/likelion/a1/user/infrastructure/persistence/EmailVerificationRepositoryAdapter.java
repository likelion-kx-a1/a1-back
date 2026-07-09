package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.EmailVerification;
import com.likelion.a1.user.domain.repository.EmailVerificationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationRepositoryAdapter implements EmailVerificationRepository {
  private final SpringDataEmailVerificationRepository repository;

  public EmailVerificationRepositoryAdapter(SpringDataEmailVerificationRepository repository) {
    this.repository = repository;
  }

  @Override
  public EmailVerification save(EmailVerification verification) {
    return repository.save(verification);
  }

  @Override
  public Optional<EmailVerification> findLatest(String email, String purpose) {
    return repository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(email, purpose);
  }
}
