package com.likelion.a1.library.domain.model;
import jakarta.persistence.*; import lombok.*; import java.io.Serializable;
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode @Embeddable
public class MediaTagId implements Serializable { private Long mediaAssetId; private Long tagId; }
