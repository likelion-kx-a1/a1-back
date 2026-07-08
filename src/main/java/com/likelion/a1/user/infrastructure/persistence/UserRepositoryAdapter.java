package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.UserRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

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
}
