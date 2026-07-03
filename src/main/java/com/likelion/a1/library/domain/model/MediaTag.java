package com.likelion.a1.library.domain.model;
import jakarta.persistence.*; import lombok.*; import java.time.OffsetDateTime;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="media_tags")
public class MediaTag { @EmbeddedId private MediaTagId id; @Column(nullable=false) private OffsetDateTime createdAt; }
