package com.likelion.a1.user.domain;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="user_cache_settings")
public class UserCacheSetting {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private Long userId;
 @Column(nullable=false) private boolean cacheGeneratedMedia, cachePromptHistory, cacheLibrary, cacheSearchResult;
 @Column(nullable=false) private int cacheDurationSeconds;
 private OffsetDateTime lastCacheClearedAt;
 @Column(nullable=false) private OffsetDateTime createdAt, updatedAt;
}
