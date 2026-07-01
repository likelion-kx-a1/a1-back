package com.likelion.a1.library.presentation.dto;
import java.time.OffsetDateTime;
public final class LibraryDtos {
 private LibraryDtos(){}
 public record CreateFolderRequest(Long parentFolderId,String name){}
 public record FolderResponse(Long id,Long parentFolderId,String name,OffsetDateTime createdAt){}
 public record CreateTagRequest(String name,String color){}
 public record TagResponse(Long id,String name,String color){}
 public record FavoriteResponse(Long id,Long mediaAssetId,OffsetDateTime createdAt){}
}
