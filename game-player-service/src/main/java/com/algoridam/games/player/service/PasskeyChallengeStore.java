package com.algoridam.games.player.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.stereotype.Component;

@Component
public class PasskeyChallengeStore {

  private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
  private static final long MAX_CHALLENGES = 10_000;

  private final Cache<String, RegistrationChallenge> registrations =
      Caffeine.newBuilder().maximumSize(MAX_CHALLENGES).expireAfterWrite(CHALLENGE_TTL).build();
  private final Cache<String, LoginChallenge> logins =
      Caffeine.newBuilder().maximumSize(MAX_CHALLENGES).expireAfterWrite(CHALLENGE_TTL).build();

  public String saveSignupRegistration(
      UUID playerId, String label, PublicKeyCredentialCreationOptions options) {
    return saveRegistration(new RegistrationChallenge(playerId, label, true, options));
  }

  public String savePasskeyRegistration(
      UUID playerId, String label, PublicKeyCredentialCreationOptions options) {
    return saveRegistration(new RegistrationChallenge(playerId, label, false, options));
  }

  private String saveRegistration(RegistrationChallenge challenge) {
    String requestId = UUID.randomUUID().toString();
    registrations.put(requestId, challenge);
    return requestId;
  }

  public RegistrationChallenge consumeRegistration(String requestId) {
    RegistrationChallenge challenge = registrations.asMap().remove(requestId);
    if (challenge == null) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Registration challenge expired");
    }
    return challenge;
  }

  public String saveLogin(PublicKeyCredentialRequestOptions options) {
    String requestId = UUID.randomUUID().toString();
    logins.put(requestId, new LoginChallenge(options));
    return requestId;
  }

  public LoginChallenge consumeLogin(String requestId) {
    LoginChallenge challenge = logins.asMap().remove(requestId);
    if (challenge == null) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Login challenge expired");
    }
    return challenge;
  }

  public record RegistrationChallenge(
      UUID playerId, String label, boolean signup, PublicKeyCredentialCreationOptions options) {}

  public record LoginChallenge(PublicKeyCredentialRequestOptions options) {}
}
