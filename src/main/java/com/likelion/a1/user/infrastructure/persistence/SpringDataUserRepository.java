package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
  Optional<User> findByLoginId(String loginId);
  // 이메일을 찾는 경우 대소문자를 구분하지 않고 찾음
  //-> 완전히 같은 거 찾음Optional<User> findByEmail(String email);
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByLoginId(String loginId);

  boolean existsByEmailIgnoreCase(String email);
}
