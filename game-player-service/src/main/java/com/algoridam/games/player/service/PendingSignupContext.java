package com.algoridam.games.player.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PendingSignupContext {

  private final ThreadLocal<PendingSignup> current = new ThreadLocal<>();

  public void set(UUID playerId, String handle, String displayName) {
    current.set(new PendingSignup(playerId, handle, displayName));
  }

  public Optional<PendingSignup> current() {
    return Optional.ofNullable(current.get());
  }

  public Optional<PendingSignup> findById(UUID playerId) {
    return current().filter(signup -> signup.playerId().equals(playerId));
  }

  public Optional<PendingSignup> findByHandle(String handle) {
    return current().filter(signup -> signup.handle().equals(handle));
  }

  public void clear() {
    current.remove();
  }

  public record PendingSignup(UUID playerId, String handle, String displayName) {}
}
