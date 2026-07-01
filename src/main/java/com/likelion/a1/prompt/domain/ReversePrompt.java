package com.likelion.a1.prompt.domain;
import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="reverse_prompts")
public class ReversePrompt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 private Long mediaAssetId, jobId, inputFileId;
 @Column(nullable=false,columnDefinition="text") private String extractedPrompt;
 @Column(columnDefinition="text") private String styleKeywords,lightingKeywords,cameraKeywords,compositionKeywords,subjectKeywords,colorKeywords;
 private BigDecimal confidenceScore;
 @Column(nullable=false) private String provider, modelName;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
