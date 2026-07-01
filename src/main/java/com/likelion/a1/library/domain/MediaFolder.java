package com.likelion.a1.library.domain;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="media_folders")
public class MediaFolder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId; private Long parentFolderId;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private OffsetDateTime createdAt, updatedAt;
}
