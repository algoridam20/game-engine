package com.algoridam.games.service.security;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentPlayerJwts {

  private CurrentPlayerJwts() {}

  public static PlayerJwtInfo require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof PlayerJwtInfo jwt)) {
      throw new ServiceException(ErrorCode.UNAUTHORIZED);
    }
    return jwt;
  }
}
