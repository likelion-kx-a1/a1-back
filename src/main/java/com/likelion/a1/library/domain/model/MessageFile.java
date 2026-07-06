package com.likelion.a1.library.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "message_files")
public class MessageFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long messageId;

  private String fileType;

  @Column(nullable = false)
  private String bucketName;

  @Column(nullable = false, columnDefinition = "text")
  private String storagePath;

  @Column(columnDefinition = "text")
  private String publicUrl;

  private String originalFilename;

  private String mimeType;
  private Long fileSize;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
