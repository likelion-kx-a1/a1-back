package com.likelion.a1.user.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private RedisRateLimiter rateLimiter;

  @BeforeEach
  void setUp() {
    rateLimiter = new RedisRateLimiter(redisTemplate);
  }

  @Test
  void increment은_최초_호출에서만_만료시간을_설정한다() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("key")).thenReturn(1L);

    long count = rateLimiter.increment("key", Duration.ofMinutes(1));

    assertThat(count).isEqualTo(1L);
    verify(redisTemplate).expire(eq("key"), eq(Duration.ofMinutes(1)));
  }

  @Test
  void increment은_두번째_호출부터_만료시간을_다시_설정하지_않는다() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("key")).thenReturn(2L);

    long count = rateLimiter.increment("key", Duration.ofMinutes(1));

    assertThat(count).isEqualTo(2L);
    verify(redisTemplate, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void tryConsume은_한도_이내면_true를_반환한다() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("key")).thenReturn(5L);

    assertThat(rateLimiter.tryConsume("key", 5, Duration.ofMinutes(1))).isTrue();
  }

  @Test
  void tryConsume은_한도를_넘으면_false를_반환한다() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("key")).thenReturn(6L);

    assertThat(rateLimiter.tryConsume("key", 5, Duration.ofMinutes(1))).isFalse();
  }

  @Test
  void lock_이후_isLocked는_true다() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.hasKey("lock-key")).thenReturn(true);

    rateLimiter.lock("lock-key", Duration.ofMinutes(15));

    verify(valueOperations).set("lock-key", "1", Duration.ofMinutes(15));
    assertThat(rateLimiter.isLocked("lock-key")).isTrue();
  }

  @Test
  void isLocked는_키가_없으면_false다() {
    when(redisTemplate.hasKey("lock-key")).thenReturn(false);

    assertThat(rateLimiter.isLocked("lock-key")).isFalse();
  }

  @Test
  void reset은_키를_삭제한다() {
    rateLimiter.reset("key");

    verify(redisTemplate).delete("key");
  }

  @Test
  void Redis_연결_실패_시_increment는_fail_open으로_0을_반환한다() {
    when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

    assertThat(rateLimiter.increment("key", Duration.ofMinutes(1))).isZero();
  }

  @Test
  void Redis_연결_실패_시_isLocked는_fail_open으로_false를_반환한다() {
    when(redisTemplate.hasKey("key")).thenThrow(new RedisConnectionFailureException("down"));

    assertThat(rateLimiter.isLocked("key")).isFalse();
  }

  @Test
  void Redis_연결_실패_시_reset은_예외를_전파하지_않는다() {
    org.mockito.Mockito.doThrow(new RedisConnectionFailureException("down"))
        .when(redisTemplate)
        .delete("key");

    rateLimiter.reset("key");
  }
}
