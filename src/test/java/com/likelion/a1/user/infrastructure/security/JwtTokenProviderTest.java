package com.likelion.a1.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.likelion.a1.user.domain.model.User;
import io.jsonwebtoken.JwtException;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
  private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";

  private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600);

  private User newUser() {
    User user =
        User.signup("tester", "tester@example.com", "hash", "테스터", LocalDate.of(2000, 1, 1), "010-0000-0000");
    setId(user, 1L);
    return user;
  }

  /** User.id는 @GeneratedValue라 실제 저장 없이는 테스트에서 채울 수 없어 리플렉션으로 강제 주입한다. */
  private void setId(User user, Long id) {
    try {
      var field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Test
  void createAccessToken_그리고_parse는_원래_값을_그대로_복원한다() {
    User user = newUser();
    String sessionId = "session-123";

    String token = jwtTokenProvider.createAccessToken(user, sessionId);
    JwtPrincipal principal = jwtTokenProvider.parse(token);

    assertThat(principal.loginId()).isEqualTo("tester");
    assertThat(principal.role()).isEqualTo("USER");
    assertThat(principal.sessionId()).isEqualTo(sessionId);
  }

  @Test
  void 다른_키로_서명된_토큰은_파싱에_실패한다() {
    JwtTokenProvider otherProvider = new JwtTokenProvider("different-secret-key-must-be-32-bytes!!", 3600);
    String token = otherProvider.createAccessToken(newUser(), "session-123");

    assertThatThrownBy(() -> jwtTokenProvider.parse(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 이미_만료된_토큰은_파싱에_실패한다() {
    JwtTokenProvider expiredIssuer = new JwtTokenProvider(SECRET, -1);
    String token = expiredIssuer.createAccessToken(newUser(), "session-123");

    assertThatThrownBy(() -> jwtTokenProvider.parse(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void 변조된_토큰은_파싱에_실패한다() {
    String token = jwtTokenProvider.createAccessToken(newUser(), "session-123");
    String[] parts = token.split("\\.");
    byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
    signature[0] ^= 0x01;
    String tamperedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

    assertThatThrownBy(() -> jwtTokenProvider.parse(tampered)).isInstanceOf(JwtException.class);
  }
}
