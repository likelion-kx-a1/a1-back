package com.likelion.a1.chat.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "chat_message_files")
public class ChatMessageFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long messageId;

  @Column(length = 30)
  private String fileType;

  @Column(nullable = false, length = 100)
  private String bucketName;

  @Column(nullable = false, columnDefinition = "text")
  private String storagePath;

  @Column(columnDefinition = "text")
  private String publicUrl;

  private String originalFilename;
  private String storedFilename;

  @Column(length = 100)
  private String mimeType;

  private Long fileSize;
  private Integer width;
  private Integer height;
  private Integer durationSeconds;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
