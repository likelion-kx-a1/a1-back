package com.likelion.a1.user.application.service;

import com.likelion.a1.global.exception.BusinessException;
import com.likelion.a1.global.exception.ErrorCode;
import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserCommandService(UserRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public User register(String email, String password, String nickname) {
    if (repository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATE);
    }
    return repository.save(User.local(email, passwordEncoder.encode(password), nickname));
  }
}
