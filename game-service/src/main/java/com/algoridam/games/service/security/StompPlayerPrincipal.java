package com.algoridam.games.service.security;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import java.security.Principal;

public record StompPlayerPrincipal(PlayerJwtInfo jwt) implements Principal {

  @Override
  public String getName() {
    return jwt.playerId().toString();
  }
}
