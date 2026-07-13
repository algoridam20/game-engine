package com.algoridam.games.player.security;

import com.algoridam.games.common.auth.InvalidPlayerJwtException;
import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.auth.PlayerJwtValidator;
import com.algoridam.games.player.config.AuthProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService implements PlayerJwtValidator {

  private static final String ALGORITHM = "HS256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final AuthProperties properties;
  private final ObjectMapper objectMapper;
  private final JwtKeyRegistry keyRegistry;

  public String generate(UUID playerId, String handle, String displayName, UUID gameId) {
    int version = keyRegistry.getActiveVersion();
    Instant now = Instant.now();
    Map<String, Object> header = Map.of("alg", ALGORITHM, "typ", "JWT", "ver", version);
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("ver", version);
    claims.put("iss", properties.jwt().issuer());
    claims.put("sub", playerId.toString());
    claims.put("handle", handle);
    claims.put("displayName", displayName);
    if (gameId != null) {
      claims.put("gameId", gameId.toString());
    }
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", now.plus(properties.jwt().ttl()).getEpochSecond());

    String unsigned = encodeJson(header) + "." + encodeJson(claims);
    return unsigned
        + "."
        + ENCODER.encodeToString(
            keyRegistry.sign(unsigned, keyRegistry.getKeysByVersion().get(version)));
  }

  @Override
  public PlayerJwtInfo validate(String token) {
    try {
      if (token == null || token.isBlank()) {
        throw invalid("JWT is missing");
      }
      String[] parts = token.split("\\.", -1);
      if (parts.length != 3) {
        throw invalid("JWT structure is invalid");
      }

      Map<String, Object> header = decodeJson(parts[0]);
      if (!ALGORITHM.equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
        throw invalid("JWT header is invalid");
      }
      int headerVersion = requiredNumber(header, "ver").intValue();
      SecretKeySpec key = keyRegistry.getKeysByVersion().get(headerVersion);
      if (key == null) {
        throw invalid("JWT version is not supported");
      }

      String unsigned = parts[0] + "." + parts[1];
      byte[] expectedSignature = keyRegistry.sign(unsigned, key);
      byte[] actualSignature = DECODER.decode(parts[2]);
      if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
        throw invalid("JWT signature is invalid");
      }

      Map<String, Object> claims = decodeJson(parts[1]);
      int payloadVersion = requiredNumber(claims, "ver").intValue();
      if (payloadVersion != headerVersion) {
        throw invalid("JWT versions do not match");
      }
      if (!properties.jwt().issuer().equals(requiredString(claims, "iss"))) {
        throw invalid("JWT issuer is invalid");
      }

      Instant issuedAt = Instant.ofEpochSecond(requiredNumber(claims, "iat").longValue());
      Instant expiresAt = Instant.ofEpochSecond(requiredNumber(claims, "exp").longValue());
      Instant now = Instant.now();
      if (issuedAt.isAfter(now.plusSeconds(60))
          || !expiresAt.isAfter(now)
          || !expiresAt.isAfter(issuedAt)) {
        throw invalid("JWT timestamps are invalid");
      }

      return new PlayerJwtInfo(
          payloadVersion,
          UUID.fromString(requiredString(claims, "sub")),
          requiredString(claims, "handle"),
          requiredString(claims, "displayName"),
          optionalUuid(claims, "gameId"),
          issuedAt,
          expiresAt);
    } catch (InvalidPlayerJwtException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new InvalidPlayerJwtException("JWT is invalid", exception);
    }
  }

  private Map<String, Object> decodeJson(String part) throws Exception {
    return objectMapper.readValue(DECODER.decode(part), new TypeReference<>() {});
  }

  private String requiredString(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      throw invalid("JWT claim is missing: " + name);
    }
    return stringValue;
  }

  private Number requiredNumber(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (!(value instanceof Number numberValue)) {
      throw invalid("JWT claim is missing: " + name);
    }
    return numberValue;
  }

  private UUID optionalUuid(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (value == null) {
      return null;
    }
    if (!(value instanceof String stringValue)) {
      throw invalid("JWT claim is invalid: " + name);
    }
    return UUID.fromString(stringValue);
  }

  private InvalidPlayerJwtException invalid(String message) {
    return new InvalidPlayerJwtException(message);
  }

  private String encodeJson(Object value) {
    try {
      return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to encode JWT", exception);
    }
  }
}
