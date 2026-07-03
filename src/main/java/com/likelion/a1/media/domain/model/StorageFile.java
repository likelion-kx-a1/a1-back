package com.likelion.a1.media.domain.model;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="storage_files")
public class StorageFile {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long mediaAssetId;
 @Column(nullable=false) private String fileType, storageProvider;
 private String bucketName;
 @Column(nullable=false,columnDefinition="text") private String storagePath;
 @Column(columnDefinition="text") private String publicUrl;
 private String originalFilename;
 @Column(nullable=false) private String mimeType;
 private Long fileSize; private String checksum; private Integer width,height,durationSeconds;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
