package com.likelion.a1.prompt.presentation.dto;
import java.math.BigDecimal; import java.time.OffsetDateTime;
public final class PromptDtos {
 private PromptDtos(){}
 public record CreateTemplateRequest(String title,String description,String templateText,String mediaType,String category,boolean publicTemplate){}
 public record TemplateResponse(Long id,String title,String description,String templateText,String mediaType,String category,boolean publicTemplate,OffsetDateTime createdAt){}
 public record ReversePromptResponse(Long id,String extractedPrompt,String styleKeywords,String lightingKeywords,String cameraKeywords,String compositionKeywords,BigDecimal confidenceScore,OffsetDateTime createdAt){}
}
