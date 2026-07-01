package com.likelion.a1.operation.presentation.dto;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.Map;
public final class OperationDtos {
 private OperationDtos(){}
 public record UsageResponse(Long id,String provider,String modelName,int requestCount,Integer inputTokens,Integer outputTokens,BigDecimal estimatedCost,String currency,String status,OffsetDateTime createdAt){}
 public record AuditResponse(Long id,Long userId,String action,String targetType,Long targetId,Map<String,Object> metadata,OffsetDateTime createdAt){}
}
