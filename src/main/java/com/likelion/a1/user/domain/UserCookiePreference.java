package com.likelion.a1.user.domain;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="user_cookie_preferences")
public class UserCookiePreference {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private Long userId;
 @Column(nullable=false) private boolean necessaryCookie, analyticsCookie, marketingCookie, preferenceCookie;
 @Column(nullable=false) private String consentVersion;
 private OffsetDateTime consentedAt;
 @Column(nullable=false) private OffsetDateTime updatedAt;
}
