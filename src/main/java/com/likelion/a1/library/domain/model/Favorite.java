package com.likelion.a1.library.domain.model;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="favorites")
public class Favorite {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId, mediaAssetId;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
