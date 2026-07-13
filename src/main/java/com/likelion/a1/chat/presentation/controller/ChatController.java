package com.likelion.a1.chat.presentation.controller;

import com.likelion.a1.chat.application.service.ChatService;
import com.likelion.a1.chat.presentation.dto.ChatDtos.ChatResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateChatRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateChatRequest;
import com.likelion.a1.global.response.ApiResponse;
import com.likelion.a1.user.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping("/api/chats")
  public ApiResponse<ChatResponse> create(
      @AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody CreateChatRequest request) {
    return ApiResponse.success(
        "CHAT_CREATED",
        "채팅이 생성되었습니다.",
        chatService.create(principal.userId(), request));
  }

  @PostMapping("/api/projects/{projectId}/chats")
  public ApiResponse<ChatResponse> createInProject(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long projectId,
      @Valid @RequestBody CreateChatRequest request) {
    return ApiResponse.success(
        "CHAT_CREATED",
        "채팅이 생성되었습니다.",
        chatService.createInProject(principal.userId(), projectId, request));
  }

  @GetMapping("/api/chats")
  public ApiResponse<List<ChatResponse>> getChats(
      @AuthenticationPrincipal JwtPrincipal principal,
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Boolean outsideProject) {
    return ApiResponse.success(
        "CHATS_FETCHED",
        "채팅 목록을 조회했습니다.",
        chatService.getChats(principal.userId(), projectId, outsideProject));
  }

  @GetMapping("/api/chats/standalone")
  public ApiResponse<List<ChatResponse>> getStandaloneChats(
      @AuthenticationPrincipal JwtPrincipal principal) {
    return ApiResponse.success(
        "CHATS_FETCHED",
        "채팅 목록을 조회했습니다.",
        chatService.getChats(principal.userId(), null, true));
  }

  @GetMapping("/api/projects/{projectId}/chats")
  public ApiResponse<List<ChatResponse>> getProjectChats(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long projectId) {
    return ApiResponse.success(
        "CHATS_FETCHED",
        "채팅 목록을 조회했습니다.",
        chatService.getChats(principal.userId(), projectId, false));
  }

  @GetMapping("/api/chats/{chatId}")
  public ApiResponse<ChatResponse> getChat(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long chatId) {
    return ApiResponse.success(
        "CHAT_FETCHED", "채팅 상세입니다.", chatService.getChat(principal.userId(), chatId));
  }

  @PatchMapping("/api/chats/{chatId}")
  public ApiResponse<ChatResponse> update(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @Valid @RequestBody UpdateChatRequest request) {
    return ApiResponse.success(
        "CHAT_UPDATED",
        "채팅 제목이 수정되었습니다.",
        chatService.update(principal.userId(), chatId, request));
  }

  @DeleteMapping("/api/chats/{chatId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long chatId) {
    chatService.delete(principal.userId(), chatId);

    return ApiResponse.success("CHAT_DELETED", "채팅이 삭제되었습니다.", null);
  }
}
