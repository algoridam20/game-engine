package com.algoridam.games.player.security;

import com.algoridam.games.common.auth.InvalidPlayerJwtException;
import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.auth.PlayerJwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final PlayerJwtValidator playerJwtValidator;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return isWebSocketPath(request.getServletPath()) || isWebSocketPath(request.getRequestURI());
  }

  private static boolean isWebSocketPath(String path) {
    return path != null && ("/ws".equals(path) || path.startsWith("/ws/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      try {
        PlayerJwtInfo player = playerJwtValidator.validate(authorization.substring(7));
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    player, null, AuthorityUtils.NO_AUTHORITIES));
      } catch (InvalidPlayerJwtException exception) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }
}
