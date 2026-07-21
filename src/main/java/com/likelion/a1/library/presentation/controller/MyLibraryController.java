package com.likelion.a1.library.presentation.controller;

import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.library.application.service.MyLibraryService;
import com.likelion.a1.media.presentation.dto.MediaDtos.CreateLibraryProjectRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.CreateStorageFolderRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectContentsResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.LibraryProjectResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.SaveAssetRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.SavedAssetResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.StorageFolderResponse;
import com.likelion.a1.media.presentation.dto.MediaDtos.UpdateLibraryProjectRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.UpdateSavedAssetRequest;
import com.likelion.a1.media.presentation.dto.MediaDtos.UpdateStorageFolderRequest;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library")
public class MyLibraryController {
  private final MyLibraryService myLibraryService;

  public MyLibraryController(MyLibraryService myLibraryService) {
    this.myLibraryService = myLibraryService;
  }

  @PostMapping("/projects")
  public ApiResponse<LibraryProjectResponse> createLibraryProject(
      @AuthenticationPrincipal JwtPrincipal principal,
      @RequestBody CreateLibraryProjectRequest request) {
    return ApiResponse.success(
        "LIBRARY_PROJECT_CREATED",
        "라이브러리 프로젝트가 생성되었습니다.",
        myLibraryService.createLibraryProject(principal.userId(), request));
  }

  @GetMapping("/projects")
  public ApiResponse<List<LibraryProjectResponse>> getLibraryProjects(
      @AuthenticationPrincipal JwtPrincipal principal,
      @RequestParam(required = false) Long parentProjectId) {
    return ApiResponse.success(
        "LIBRARY_PROJECTS_FETCHED",
        "라이브러리 프로젝트 목록을 조회했습니다.",
        myLibraryService.getLibraryProjects(principal.userId(), parentProjectId));
  }

  @PatchMapping("/projects/{libraryProjectId}")
  public ApiResponse<LibraryProjectResponse> updateLibraryProject(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long libraryProjectId,
      @RequestBody UpdateLibraryProjectRequest request) {
    return ApiResponse.success(
        "LIBRARY_PROJECT_UPDATED",
        "라이브러리 프로젝트가 수정되었습니다.",
        myLibraryService.updateLibraryProject(principal.userId(), libraryProjectId, request));
  }

  @GetMapping("/projects/{libraryProjectId}/contents")
  public ApiResponse<LibraryProjectContentsResponse> getLibraryProjectContents(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long libraryProjectId,
      @RequestParam(required = false) Long folderId,
      @RequestParam(required = false) String assetType,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.success(
        "LIBRARY_PROJECT_CONTENTS_FETCHED",
        "라이브러리 프로젝트 내용을 조회했습니다.",
        myLibraryService.getLibraryProjectContents(
            principal.userId(), libraryProjectId, folderId, assetType, keyword));
  }

  @DeleteMapping("/projects/{libraryProjectId}")
  public ApiResponse<Void> deleteLibraryProject(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long libraryProjectId) {
    myLibraryService.deleteLibraryProject(principal.userId(), libraryProjectId);

    return ApiResponse.success("LIBRARY_PROJECT_DELETED", "라이브러리 프로젝트가 삭제되었습니다.", null);
  }

  @PostMapping("/projects/{libraryProjectId}/folders")
  public ApiResponse<StorageFolderResponse> createFolder(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long libraryProjectId,
      @RequestBody CreateStorageFolderRequest request) {
    return ApiResponse.success(
        "LIBRARY_FOLDER_CREATED",
        "라이브러리 폴더가 생성되었습니다.",
        myLibraryService.createFolder(principal.userId(), libraryProjectId, request));
  }

  @PatchMapping("/folders/{folderId}")
  public ApiResponse<StorageFolderResponse> updateFolder(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long folderId,
      @RequestBody UpdateStorageFolderRequest request) {
    return ApiResponse.success(
        "LIBRARY_FOLDER_UPDATED",
        "라이브러리 폴더가 수정되었습니다.",
        myLibraryService.updateFolder(principal.userId(), folderId, request));
  }

  @DeleteMapping("/folders/{folderId}")
  public ApiResponse<Void> deleteFolder(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long folderId) {
    myLibraryService.deleteFolder(principal.userId(), folderId);

    return ApiResponse.success("LIBRARY_FOLDER_DELETED", "라이브러리 폴더가 삭제되었습니다.", null);
  }

  @PostMapping("/assets")
  public ApiResponse<SavedAssetResponse> saveAsset(
      @AuthenticationPrincipal JwtPrincipal principal, @RequestBody SaveAssetRequest request) {
    return ApiResponse.success(
        "LIBRARY_ASSET_SAVED",
        "에셋이 라이브러리에 저장되었습니다.",
        myLibraryService.saveAsset(principal.userId(), request));
  }

  @GetMapping("/assets")
  public ApiResponse<List<SavedAssetResponse>> getSavedAssets(
      @AuthenticationPrincipal JwtPrincipal principal,
      @RequestParam(required = false) Long libraryProjectId,
      @RequestParam(required = false) String assetType,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.success(
        "LIBRARY_ASSETS_FETCHED",
        "저장된 에셋 목록을 조회했습니다.",
        myLibraryService.getSavedAssets(principal.userId(), libraryProjectId, assetType, keyword));
  }

  @GetMapping("/assets/{savedAssetId}")
  public ApiResponse<SavedAssetResponse> getSavedAsset(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long savedAssetId) {
    return ApiResponse.success(
        "LIBRARY_ASSET_FETCHED",
        "저장된 에셋을 조회했습니다.",
        myLibraryService.getSavedAsset(principal.userId(), savedAssetId));
  }

  @PatchMapping("/assets/{savedAssetId}")
  public ApiResponse<SavedAssetResponse> updateSavedAsset(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long savedAssetId,
      @RequestBody UpdateSavedAssetRequest request) {
    return ApiResponse.success(
        "LIBRARY_ASSET_UPDATED",
        "저장된 에셋이 수정되었습니다.",
        myLibraryService.updateSavedAsset(principal.userId(), savedAssetId, request));
  }

  @DeleteMapping("/assets/{savedAssetId}")
  public ApiResponse<Void> deleteSavedAsset(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long savedAssetId) {
    myLibraryService.deleteSavedAsset(principal.userId(), savedAssetId);

    return ApiResponse.success("LIBRARY_ASSET_DELETED", "저장된 에셋이 삭제되었습니다.", null);
  }
}
