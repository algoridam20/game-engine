package com.algoridam.games.service.controller;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.service.model.ChatMessage;
import com.algoridam.games.service.security.StompAuth;
import com.algoridam.games.service.service.GameRoomManager;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class GameRoomRealtimeController {

  private final GameRoomManager gameRoomManager;

  @MessageMapping("/chat/join")
  public void joinChat(final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    Map<String, Object> sessionAttribute =
        Objects.requireNonNull(headerAccessor.getSessionAttributes());
    sessionAttribute.put(StompAuth.ROOM_ID, jwt.gameId());
    sessionAttribute.put(StompAuth.PLAYER_ID, jwt.playerId());
    sessionAttribute.put(StompAuth.DISPLAY_NAME, jwt.displayName());
    gameRoomManager.joinRealtime(jwt);
  }

  @MessageMapping("/chat/send_message")
  public void sendMessage(
      @Payload @Valid final ChatMessage chatMessage,
      final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    gameRoomManager.sendMessage(chatMessage.getMessage(), jwt);
  }

  @MessageMapping("/chat/abandon")
  public void abandon(final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    gameRoomManager.abandonRoom(jwt);
  }
}
