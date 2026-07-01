package com.likelion.a1.generation.domain;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="ai_models")
public class AiModel {
 @Id @GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String provider;
 @Column(nullable=false,unique=true) private String modelName;
 @Column(nullable=false) private String displayName, mediaType, taskType;
 @Column(nullable=false) private boolean isActive, isDefault;
 private BigDecimal costPerRequest, costPerSecond, costPerToken;
 private Integer maxDurationSeconds, maxPromptLength;
 @Column(nullable=false) private OffsetDateTime createdAt, updatedAt;
}
