package com.likelion.a1.chat.presentation.controller;

import com.likelion.a1.chat.application.service.ChatMessageService;
import com.likelion.a1.chat.presentation.dto.ChatDtos.CreateMessageRequest;
import com.likelion.a1.chat.presentation.dto.ChatDtos.MessageResponse;
import com.likelion.a1.chat.presentation.dto.ChatDtos.UpdateMessageRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
public class ChatMessageController {
  private final ChatMessageService chatMessageService;

  public ChatMessageController(ChatMessageService chatMessageService) {
    this.chatMessageService = chatMessageService;
  }

  @PostMapping
  public ApiResponse<MessageResponse> create(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @Valid @RequestBody CreateMessageRequest request) {
    return ApiResponse.success(
        "CHAT_MESSAGE_CREATED",
        "채팅 메시지가 생성되었습니다.",
        chatMessageService.create(principal.userId(), chatId, request));
  }

  @GetMapping
  public ApiResponse<List<MessageResponse>> getMessages(
      @AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long chatId) {
    return ApiResponse.success(
        "CHAT_MESSAGES_FETCHED",
        "채팅 메시지 목록을 조회했습니다.",
        chatMessageService.getMessages(principal.userId(), chatId));
  }

  @PatchMapping("/{messageId}")
  public ApiResponse<MessageResponse> update(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long messageId,
      @Valid @RequestBody UpdateMessageRequest request) {
    return ApiResponse.success(
        "CHAT_MESSAGE_UPDATED",
        "채팅 메시지가 수정되었습니다.",
        chatMessageService.update(principal.userId(), chatId, messageId, request));
  }

  @DeleteMapping("/{messageId}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal JwtPrincipal principal,
      @PathVariable Long chatId,
      @PathVariable Long messageId) {
    chatMessageService.delete(principal.userId(), chatId, messageId);

    return ApiResponse.success("CHAT_MESSAGE_DELETED", "채팅 메시지가 삭제되었습니다.", null);
  }
}
