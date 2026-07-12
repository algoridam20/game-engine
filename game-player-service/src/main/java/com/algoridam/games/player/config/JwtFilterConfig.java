package com.algoridam.games.player.config;

import com.algoridam.games.common.auth.PlayerJwtValidator;
import com.algoridam.games.player.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterConfig {

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(PlayerJwtValidator playerJwtValidator) {
    return new JwtAuthenticationFilter(playerJwtValidator);
  }
}
