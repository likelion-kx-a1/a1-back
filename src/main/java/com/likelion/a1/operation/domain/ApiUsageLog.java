package com.likelion.a1.operation.domain;
import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.Map;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="api_usage_logs")
public class ApiUsageLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long userId; private Long jobId,aiModelId;
 @Column(nullable=false) private String provider,modelName;
 @Column(nullable=false) private int requestCount;
 private Integer inputTokens,outputTokens,generatedImageCount,generatedVideoSeconds;
 private BigDecimal estimatedCost;
 @Column(nullable=false) private String currency,status;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> pricingSnapshot;
 @Column(nullable=false) private OffsetDateTime createdAt;
}
