package com.algoridam.games.service.security;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import java.security.Principal;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

public final class StompAuth {

  public static final String ROOM_ID = "roomId";
  public static final String PLAYER_ID = "playerId";
  public static final String DISPLAY_NAME = "displayName";

  private StompAuth() {}

  public static PlayerJwtInfo requireGameJwt(SimpMessageHeaderAccessor headerAccessor) {
    Principal principal = headerAccessor.getUser();
    if (!(principal instanceof StompPlayerPrincipal playerPrincipal)
        || playerPrincipal.jwt().gameId() == null) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED);
    }
    return playerPrincipal.jwt();
  }
}
