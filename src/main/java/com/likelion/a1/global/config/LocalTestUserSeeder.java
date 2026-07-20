package com.likelion.a1.global.config;

import com.likelion.a1.user.domain.model.User;
import com.likelion.a1.user.domain.repository.UserRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * local 프로필에서만 Postman 테스트용 계정을 앱 기동 시 1회 시딩한다(이미 있으면 아무 것도 하지 않음).
 * 비밀번호는 매번 이 시점의 PasswordEncoder로 인코딩하므로 소스에 해시를 하드코딩하지 않는다.
 * prod 프로필에는 이 빈이 등록되지 않으므로 운영 DB에는 절대 생성되지 않는다.
 */
@Component
@Profile("local")
public class LocalTestUserSeeder implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(LocalTestUserSeeder.class);
  private static final String TEST_LOGIN_ID = "tester";
  private static final String TEST_PASSWORD = "password123!";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public LocalTestUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByLoginId(TEST_LOGIN_ID)) {
      return;
    }

    User user =
        User.signup(
            TEST_LOGIN_ID,
            "tester@example.com",
            passwordEncoder.encode(TEST_PASSWORD),
            "Tester",
            LocalDate.of(1990, 1, 1),
            "010-0000-0000");
    user.approve(null);

    userRepository.save(user);

    log.info("[local] Postman 테스트 계정 시딩 완료 (loginId={}, password={})", TEST_LOGIN_ID, TEST_PASSWORD);
  }
}
