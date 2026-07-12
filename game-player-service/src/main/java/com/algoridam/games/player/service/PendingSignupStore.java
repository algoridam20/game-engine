package com.algoridam.games.player.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PendingSignupStore {

  private static final Duration SIGNUP_TTL = Duration.ofMinutes(5);
  private static final long MAX_PENDING_SIGNUPS = 10_000;

  private final Map<String, UUID> idsByHandle = new ConcurrentHashMap<>();
  private final Cache<UUID, PendingSignup> byId =
      Caffeine.newBuilder()
          .maximumSize(MAX_PENDING_SIGNUPS)
          .expireAfterWrite(SIGNUP_TTL)
          .<UUID, PendingSignup>removalListener(
              (playerId, signup, cause) -> {
                if (playerId != null && signup != null) {
                  idsByHandle.remove(signup.handle(), playerId);
                }
              })
          .build();

  public PendingSignup save(UUID playerId, String handle, String displayName) {
    while (true) {
      UUID existingId = idsByHandle.putIfAbsent(handle, playerId);
      if (existingId == null) {
        break;
      }
      if (byId.getIfPresent(existingId) != null) {
        throw new ServiceException(ErrorCode.DUPLICATE_REQUEST, "Signup already in progress");
      }
      idsByHandle.remove(handle, existingId);
    }

    PendingSignup signup = new PendingSignup(playerId, handle, displayName);
    byId.put(playerId, signup);
    return signup;
  }

  public Optional<PendingSignup> findById(UUID playerId) {
    return Optional.ofNullable(byId.getIfPresent(playerId));
  }

  public Optional<PendingSignup> findByHandle(String handle) {
    UUID playerId = idsByHandle.get(handle);
    if (playerId == null) {
      return Optional.empty();
    }
    PendingSignup signup = byId.getIfPresent(playerId);
    if (signup == null) {
      idsByHandle.remove(handle, playerId);
    }
    return Optional.ofNullable(signup);
  }

  public void remove(UUID playerId) {
    PendingSignup removed = byId.asMap().remove(playerId);
    if (removed != null) {
      idsByHandle.remove(removed.handle(), playerId);
    }
  }

  public record PendingSignup(UUID playerId, String handle, String displayName) {}
}
