package com.likelion.a1.user.presentation.dto;
import java.time.OffsetDateTime;
public final class UserSettingsDtos {
 private UserSettingsDtos() {}
 public record UpdateCookiePreferenceRequest(boolean analyticsCookie,boolean marketingCookie,boolean preferenceCookie,String consentVersion){}
 public record CookiePreferenceResponse(Long id,Long userId,boolean necessaryCookie,boolean analyticsCookie,boolean marketingCookie,boolean preferenceCookie,String consentVersion,OffsetDateTime updatedAt){}
 public record UpdateCacheSettingRequest(boolean cacheGeneratedMedia,boolean cachePromptHistory,boolean cacheLibrary,boolean cacheSearchResult,int cacheDurationSeconds){}
 public record CacheSettingResponse(Long id,Long userId,boolean cacheGeneratedMedia,boolean cachePromptHistory,boolean cacheLibrary,boolean cacheSearchResult,int cacheDurationSeconds){}
 public record SessionResponse(Long id,String sessionId,String deviceName,String deviceType,String status,OffsetDateTime lastAccessedAt,OffsetDateTime refreshTokenExpiredAt){}
}
