package com.likelion.a1.operation.domain.model;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime; import java.util.Map;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="audit_logs")
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private Long userId,targetId;
 @Column(nullable=false) private String action;
 private String targetType;
 @Column(columnDefinition="inet") private String ipAddress;
 @Column(columnDefinition="text") private String userAgent;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> metadata;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
