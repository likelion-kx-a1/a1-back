package com.likelion.a1.generation.presentation.dto;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.Map;
public final class GenerationManagementDtos {
 private GenerationManagementDtos(){}
 public record AiModelResponse(Long id,String provider,String modelName,String displayName,String mediaType,String taskType,boolean active,boolean defaultModel,BigDecimal costPerRequest){}
 public record JobEventResponse(Long id,Long jobId,String previousStatus,String currentStatus,String message,Map<String,Object> metadata,OffsetDateTime createdAt){}
}
