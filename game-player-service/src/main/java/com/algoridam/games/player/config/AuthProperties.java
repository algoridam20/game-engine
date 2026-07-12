package com.algoridam.games.player.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game.auth")
public record AuthProperties(Passkey passkey, Jwt jwt) {

  public record Passkey(String rpId, String rpName, String origin) {}

  public record Jwt(String issuer, Duration ttl, int activeVersion, List<JwtVersion> versions) {}

  public record JwtVersion(int version, String secret) {}
}
