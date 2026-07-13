package com.algoridam.games.player.security;

import com.algoridam.games.player.config.AuthProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtKeyRegistry {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final AuthProperties properties;
  @Getter private Map<Integer, SecretKeySpec> keysByVersion;
  @Getter private int activeVersion;

  @PostConstruct
  void validateConfiguration() {
    AuthProperties.Jwt jwt = properties.jwt();
    if (jwt == null
        || jwt.issuer() == null
        || jwt.issuer().isBlank()
        || jwt.ttl() == null
        || jwt.ttl().isNegative()
        || jwt.ttl().isZero()
        || jwt.versions() == null
        || jwt.versions().isEmpty()) {
      throw new IllegalStateException("JWT configuration is incomplete");
    }

    Map<Integer, SecretKeySpec> configuredKeys = new HashMap<>();
    for (AuthProperties.JwtVersion version : jwt.versions()) {
      if (version.version() <= 0
          || version.secret() == null
          || version.secret().length() < 16
          || configuredKeys.put(
                  version.version(),
                  new SecretKeySpec(
                      version.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM))
              != null) {
        throw new IllegalStateException("JWT versions must be unique and have valid secrets");
      }
    }
    if (!configuredKeys.containsKey(jwt.activeVersion())) {
      throw new IllegalStateException("JWT active version is not configured");
    }
    keysByVersion = Map.copyOf(configuredKeys);
    activeVersion = jwt.activeVersion();
  }

  public byte[] sign(String unsigned, SecretKeySpec key) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(key);
      return mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to sign token", exception);
    }
  }
}
