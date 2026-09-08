package com.algoridam.games.service.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChatMessage {
  @NotBlank(message = "Message is required")
  private String message;

  private String senderId;
  private Instant timeStamp;
  private MessageType type;

  public static ChatMessage createChatMessage(String senderId, String message, MessageType type) {
    return new ChatMessage(message, senderId, Instant.now(), type);
  }
}
