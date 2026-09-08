package com.algoridam.games.player.security;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.player.entity.PlayerEntity;
import com.algoridam.games.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSecurityCurrentPlayerProvider {

  private final PlayerRepository players;

  public PlayerEntity requirePlayer() {
    return players
        .findById(requireJwtInfo().playerId())
        .orElseThrow(() -> new ServiceException(ErrorCode.UNAUTHORIZED));
  }

  public PlayerJwtInfo requireJwtInfo() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED);
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof PlayerJwtInfo jwtInfo) {
      return jwtInfo;
    }
    throw new ServiceException(ErrorCode.UNAUTHORIZED);
  }
}
