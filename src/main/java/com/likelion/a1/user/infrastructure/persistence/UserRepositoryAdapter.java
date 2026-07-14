package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.UserRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {
  private final SpringDataUserRepository repository;

  public UserRepositoryAdapter(SpringDataUserRepository repository) {
    this.repository = repository;
  }

  @Override
  public User save(User user) {
    return repository.save(user);
  }
  @Override
  public Optional<User> findById(Long id) {
    return repository.findById(id);
  }

  @Override
  public Optional<User> findByLoginId(String loginId) {
    return repository.findByLoginId(loginId);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return repository.findByEmailIgnoreCase(email);
  }

  @Override
  public boolean existsByLoginId(String loginId) {
    return repository.existsByLoginId(loginId);
  }
  
  @Override
  public boolean existsByEmail(String email) {
    return repository.existsByEmailIgnoreCase(email);
  }

  @Override
  public Page<User> findSignupRequests(Pageable pageable) {
    return repository.findByApprovalStatusAndDeletedAtIsNullOrderByCreatedAtDesc("PENDING", pageable);
  }

  @Override
  public Page<User> searchUsers(
      String approvalStatus, String accountStatus, String keyword, Pageable pageable) {
    if (keyword == null) {
      if (approvalStatus != null && accountStatus != null) {
        return repository.findByApprovalStatusAndAccountStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            approvalStatus, accountStatus, pageable);
      }

      if (approvalStatus != null) {
        return repository.findByApprovalStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            approvalStatus, pageable);
      }

      if (accountStatus != null) {
        return repository.findByAccountStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            accountStatus, pageable);
      }

      return repository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
    }

    return repository.searchUsers(approvalStatus, accountStatus, keyword, pageable);
  }
}
