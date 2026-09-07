package com.algoridam.games.service.security;

import com.algoridam.games.common.auth.InvalidPlayerJwtException;
import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.auth.PlayerJwtValidator;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private final PlayerJwtValidator playerJwtValidator;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }
    PlayerJwtInfo jwt = validateGameScopedJwt(bearerToken(accessor));
    accessor.setUser(new StompPlayerPrincipal(jwt));
    return message;
  }

  private String bearerToken(StompHeaderAccessor accessor) {
    String header = accessor.getFirstNativeHeader("Authorization");
    if (header == null) {
      header = accessor.getFirstNativeHeader("authorization");
    }
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED);
    }
    return header.substring("Bearer ".length());
  }

  private PlayerJwtInfo validateGameScopedJwt(String token) {
    try {
      PlayerJwtInfo jwt = playerJwtValidator.validate(token);
      if (jwt.gameId() == null) {
        throw new ServiceException(ErrorCode.UNAUTHORIZED, "Game-scoped JWT is required");
      }
      return jwt;
    } catch (InvalidPlayerJwtException exception) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED);
    }
  }
}
