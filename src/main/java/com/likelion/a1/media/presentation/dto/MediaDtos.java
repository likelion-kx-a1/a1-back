package com.likelion.a1.media.presentation.dto;
import java.time.OffsetDateTime; import java.util.*;
public final class MediaDtos {
 private MediaDtos(){}
 public record MediaAssetResponse(UUID publicId,String mediaType,String title,String originalPrompt,String provider,String modelName,String status,String visibility,Integer width,Integer height,Integer durationSeconds,OffsetDateTime createdAt){}
 public record StorageFileResponse(Long id,String fileType,String storageProvider,String mimeType,Long fileSize,Integer width,Integer height,Integer durationSeconds){}
 public record MediaVersionResponse(Long id,int versionNumber,String prompt,String modelName,String changeType,String changeNote,OffsetDateTime createdAt){}
}
