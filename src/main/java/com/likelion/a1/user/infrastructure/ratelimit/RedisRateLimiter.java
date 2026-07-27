package com.likelion.a1.user.infrastructure.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis INCR+EXPIRE 기반 고정 윈도우(fixed-window) 카운터/잠금 플래그 유틸리티(docs_h/보안_취약점_점검.md
 * #5). 로그인 브루트포스, 이메일 인증코드 발송/검증 등에서 공통으로 쓴다.
 *
 * <p>이 레이트리밋은 심층 방어(defense-in-depth) 계층이지 유일한 방어선이 아니므로, Redis 연결
 * 자체가 끊겨도 로그인/가입 전체를 막는 새로운 단일 장애점이 되지 않도록 모든 메서드가 fail-open
 * (허용/미잠금으로 간주)하고 경고 로그만 남긴다.
 */
@Component
public class RedisRateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

  private final StringRedisTemplate redisTemplate;

  public RedisRateLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /** key의 카운터를 1 증가시키고(최초 증가 시 window로 만료 설정) 새 카운트를 반환한다. */
  public long increment(String key, Duration window) {
    try {
      Long count = redisTemplate.opsForValue().increment(key);
      if (count == null) {
        return 0L;
      }
      if (count == 1L) {
        redisTemplate.expire(key, window);
      }
      return count;
    } catch (DataAccessException exception) {
      log.warn("Redis 카운터 증가 실패, 첫 시도로 간주합니다(fail-open). key={}", key, exception);
      return 0L;
    }
  }

  /** increment(key, window)의 결과가 maxAttempts 이하이면 true(허용). */
  public boolean tryConsume(String key, long maxAttempts, Duration window) {
    return increment(key, window) <= maxAttempts;
  }

  public void reset(String key) {
    try {
      redisTemplate.delete(key);
    } catch (DataAccessException exception) {
      log.warn("Redis 카운터 초기화 실패. key={}", key, exception);
    }
  }

  public void lock(String key, Duration duration) {
    try {
      redisTemplate.opsForValue().set(key, "1", duration);
    } catch (DataAccessException exception) {
      log.warn("Redis 잠금 설정 실패. key={}", key, exception);
    }
  }

  public boolean isLocked(String key) {
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    } catch (DataAccessException exception) {
      log.warn("Redis 잠금 조회 실패, 잠기지 않은 것으로 간주합니다(fail-open). key={}", key, exception);
      return false;
    }
  }
}
