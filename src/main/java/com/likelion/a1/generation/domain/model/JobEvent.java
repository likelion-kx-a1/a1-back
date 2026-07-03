package com.likelion.a1.generation.domain.model;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime; import java.util.Map;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="job_events")
public class JobEvent {
 @Id @GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long jobId;
 private String previousStatus;
 @Column(nullable=false) private String currentStatus;
 @Column(columnDefinition="text") private String message;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> metadata;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
