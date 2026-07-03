package com.likelion.a1.media.domain.model;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="media_versions")
public class MediaVersion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long mediaAssetId;
 private Long jobId, storageFileId;
 @Column(nullable=false) private int versionNumber;
 @Column(columnDefinition="text") private String prompt;
 private String modelName, provider;
 @Column(nullable=false) private String changeType;
 @Column(columnDefinition="text") private String changeNote;
 @Column(nullable=false) private Long createdBy;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
