package com.likelion.a1.prompt.domain;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="prompt_templates")
public class PromptTemplate {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId;
 @Column(nullable=false) private String title;
 @Column(columnDefinition="text") private String description;
 @Column(nullable=false,columnDefinition="text") private String templateText;
 @Column(nullable=false) private String mediaType;
 private String category;
 @Column(nullable=false) private boolean isPublic;
 @Column(nullable=false) private OffsetDateTime createdAt,updatedAt;
}
