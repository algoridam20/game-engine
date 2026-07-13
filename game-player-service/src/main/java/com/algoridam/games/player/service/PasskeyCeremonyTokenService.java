package com.algoridam.games.player.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.player.security.JwtKeyRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.time.Duration;
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
public class PasskeyCeremonyTokenService {

  private static final String ALGORITHM = "HS256";
  private static final String TOKEN_TYPE = "PCT";
  private static final String PURPOSE_REGISTRATION = "passkey-reg";
  private static final String PURPOSE_LOGIN = "passkey-login";
  private static final Duration CEREMONY_TTL = Duration.ofMinutes(5);
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final JwtKeyRegistry keyRegistry;
  private final ObjectMapper objectMapper;

  public String issueRegistration(RegistrationChallenge challenge) {
    int version = keyRegistry.getActiveVersion();
    Instant now = Instant.now();
    Map<String, Object> header =
        Map.of(
            "alg", ALGORITHM, "typ", TOKEN_TYPE, "ver", version, "purpose", PURPOSE_REGISTRATION);
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("ver", version);
    claims.put("purpose", PURPOSE_REGISTRATION);
    claims.put("playerId", challenge.playerId().toString());
    claims.put("label", challenge.label());
    claims.put("signup", challenge.signup());
    claims.put("challenge", challenge.challenge());
    if (challenge.handle() != null) {
      claims.put("handle", challenge.handle());
    }
    if (challenge.displayName() != null) {
      claims.put("displayName", challenge.displayName());
    }
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", now.plus(CEREMONY_TTL).getEpochSecond());
    return sign(version, header, claims);
  }

  public RegistrationChallenge verifyRegistration(String token) {
    Map<String, Object> claims = verify(token, PURPOSE_REGISTRATION);
    UUID playerId = UUID.fromString(requiredString(claims, "playerId"));
    String label = requiredString(claims, "label");
    boolean signup = requiredBoolean(claims, "signup");
    String challenge = requiredString(claims, "challenge");
    String handle = optionalString(claims, "handle");
    String displayName = optionalString(claims, "displayName");
    return new RegistrationChallenge(playerId, label, signup, challenge, handle, displayName);
  }

  public String issueLogin(LoginChallenge challenge) {
    int version = keyRegistry.getActiveVersion();
    Instant now = Instant.now();
    Map<String, Object> header =
        Map.of("alg", ALGORITHM, "typ", TOKEN_TYPE, "ver", version, "purpose", PURPOSE_LOGIN);
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("ver", version);
    claims.put("purpose", PURPOSE_LOGIN);
    claims.put("challenge", challenge.challenge());
    if (challenge.handle() != null) {
      claims.put("handle", challenge.handle());
    }
    if (challenge.gameId() != null) {
      claims.put("gameId", challenge.gameId().toString());
    }
    claims.put("iat", now.getEpochSecond());
    claims.put("exp", now.plus(CEREMONY_TTL).getEpochSecond());
    return sign(version, header, claims);
  }

  public LoginChallenge verifyLogin(String token) {
    Map<String, Object> claims = verify(token, PURPOSE_LOGIN);
    String challenge = requiredString(claims, "challenge");
    String handle = optionalString(claims, "handle");
    UUID gameId = optionalUuid(claims, "gameId");
    return new LoginChallenge(challenge, handle, gameId);
  }

  private String sign(int version, Map<String, Object> header, Map<String, Object> claims) {
    String unsigned = encodeJson(header) + "." + encodeJson(claims);
    return unsigned
        + "."
        + ENCODER.encodeToString(
            keyRegistry.sign(unsigned, keyRegistry.getKeysByVersion().get(version)));
  }

  private Map<String, Object> verify(String token, String expectedPurpose) {
    if (token == null || token.isBlank()) {
      throw badRequest("Ceremony token is missing");
    }
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) {
      throw badRequest("Ceremony token structure is invalid");
    }

    Map<String, Object> header = decodeJson(parts[0]);
    if (!ALGORITHM.equals(header.get("alg"))
        || !TOKEN_TYPE.equals(header.get("typ"))
        || !expectedPurpose.equals(header.get("purpose"))) {
      throw badRequest("Ceremony token header is invalid");
    }
    int headerVersion = requiredNumber(header, "ver").intValue();
    SecretKeySpec key = keyRegistry.getKeysByVersion().get(headerVersion);
    if (key == null) {
      throw badRequest("Ceremony token version is not supported");
    }

    String unsigned = parts[0] + "." + parts[1];
    byte[] expectedSignature = keyRegistry.sign(unsigned, key);
    byte[] actualSignature;
    try {
      actualSignature = DECODER.decode(parts[2]);
    } catch (IllegalArgumentException exception) {
      throw badRequest("Ceremony token signature is invalid");
    }
    if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
      throw badRequest("Ceremony token signature is invalid");
    }

    Map<String, Object> claims = decodeJson(parts[1]);
    if (!expectedPurpose.equals(requiredString(claims, "purpose"))) {
      throw badRequest("Ceremony token purpose is invalid");
    }
    int payloadVersion = requiredNumber(claims, "ver").intValue();
    if (payloadVersion != headerVersion) {
      throw badRequest("Ceremony token versions do not match");
    }

    Instant issuedAt = Instant.ofEpochSecond(requiredNumber(claims, "iat").longValue());
    Instant expiresAt = Instant.ofEpochSecond(requiredNumber(claims, "exp").longValue());
    Instant now = Instant.now();
    if (issuedAt.isAfter(now.plusSeconds(60))
        || !expiresAt.isAfter(now)
        || !expiresAt.isAfter(issuedAt)) {
      throw badRequest("Ceremony token expired");
    }
    return claims;
  }

  private Map<String, Object> decodeJson(String part) {
    try {
      return objectMapper.readValue(DECODER.decode(part), new TypeReference<>() {});
    } catch (Exception exception) {
      throw badRequest("Ceremony token payload is invalid");
    }
  }

  private String requiredString(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      throw badRequest("Ceremony token claim is missing: " + name);
    }
    return stringValue;
  }

  private String optionalString(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (value == null) {
      return null;
    }
    if (!(value instanceof String stringValue)) {
      throw badRequest("Ceremony token claim is invalid: " + name);
    }
    return stringValue;
  }

  private UUID optionalUuid(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (value == null) {
      return null;
    }
    if (!(value instanceof String stringValue)) {
      throw badRequest("Ceremony token claim is invalid: " + name);
    }
    return UUID.fromString(stringValue);
  }

  private boolean requiredBoolean(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (!(value instanceof Boolean booleanValue)) {
      throw badRequest("Ceremony token claim is missing: " + name);
    }
    return booleanValue;
  }

  private Number requiredNumber(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (!(value instanceof Number numberValue)) {
      throw badRequest("Ceremony token claim is missing: " + name);
    }
    return numberValue;
  }

  private ServiceException badRequest(String message) {
    return new ServiceException(ErrorCode.BAD_REQUEST, message);
  }

  private String encodeJson(Object value) {
    try {
      return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to encode ceremony token", exception);
    }
  }

  public record RegistrationChallenge(
      UUID playerId,
      String label,
      boolean signup,
      String challenge,
      String handle,
      String displayName) {}

  public record LoginChallenge(String challenge, String handle, UUID gameId) {}
}
