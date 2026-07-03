package com.likelion.a1.media.domain.model;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime; import java.util.*;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="media_assets")
public class MediaAsset {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private UUID publicId;
 @Column(nullable=false) private Long userId; private Long jobId, folderId;
 @Column(nullable=false) private String mediaType;
 private String title; @Column(columnDefinition="text") private String description;
 @Column(nullable=false,columnDefinition="text") private String originalPrompt, finalPrompt;
 @Column(columnDefinition="text") private String reversePrompt;
 @Column(nullable=false) private String provider, modelName, status, visibility;
 private Integer width, height, durationSeconds; private String aspectRatio;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> metadata;
 @Column(nullable=false) private OffsetDateTime createdAt, updatedAt; private OffsetDateTime deletedAt;
}
