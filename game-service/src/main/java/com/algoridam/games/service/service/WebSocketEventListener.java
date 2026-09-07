package com.algoridam.games.service.service;

import com.algoridam.games.service.security.StompAuth;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final GameRoomManager gameRoomManager;

  @EventListener
  public void handleWebSocketEventListener(SessionDisconnectEvent disconnectEvent) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(disconnectEvent.getMessage());
    Map<String, Object> sessionAttribute = headerAccessor.getSessionAttributes();
    if (sessionAttribute == null) {
      return;
    }
    UUID roomId = (UUID) sessionAttribute.get(StompAuth.ROOM_ID);
    UUID playerId = (UUID) sessionAttribute.get(StompAuth.PLAYER_ID);
    if (roomId != null && playerId != null) {
      log.info("User: {}, disconnected from Room: {}", playerId, roomId);
      gameRoomManager.exitRoom(playerId, roomId);
    }
  }
}
