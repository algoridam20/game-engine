package com.algoridam.games.player.webauthn;

import com.algoridam.games.player.entity.PlayerEntity;
import com.algoridam.games.player.repository.PlayerRepository;
import com.algoridam.games.player.service.PendingSignupStore;
import com.algoridam.games.player.service.PendingSignupStore.PendingSignup;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlayerCredentialUserRepository implements PublicKeyCredentialUserEntityRepository {

  private final PlayerRepository players;
  private final PendingSignupStore pendingSignups;

  @Override
  public PublicKeyCredentialUserEntity findById(Bytes id) {
    UUID playerId = UUID.fromString(new String(id.getBytes(), StandardCharsets.UTF_8));
    return players
        .findById(playerId)
        .map(this::toCredentialUser)
        .or(() -> pendingSignups.findById(playerId).map(this::toCredentialUser))
        .orElse(null);
  }

  @Override
  public PublicKeyCredentialUserEntity findByUsername(String username) {
    return players
        .findByHandle(username)
        .map(this::toCredentialUser)
        .or(() -> pendingSignups.findByHandle(username).map(this::toCredentialUser))
        .orElse(null);
  }

  @Override
  public void save(PublicKeyCredentialUserEntity userEntity) {
    if (players.existsByHandle(userEntity.getName())
        || pendingSignups.findByHandle(userEntity.getName()).isPresent()) {
      return;
    }
    throw new IllegalArgumentException("Player must be created through the signup flow");
  }

  @Override
  public void delete(Bytes id) {
    UUID playerId = UUID.fromString(new String(id.getBytes(), StandardCharsets.UTF_8));
    players.deleteById(playerId);
  }

  private PublicKeyCredentialUserEntity toCredentialUser(PlayerEntity player) {
    return ImmutablePublicKeyCredentialUserEntity.builder()
        .id(new Bytes(player.getId().toString().getBytes(StandardCharsets.UTF_8)))
        .name(player.getHandle())
        .displayName(player.getDisplayName())
        .build();
  }

  private PublicKeyCredentialUserEntity toCredentialUser(PendingSignup signup) {
    return ImmutablePublicKeyCredentialUserEntity.builder()
        .id(new Bytes(signup.playerId().toString().getBytes(StandardCharsets.UTF_8)))
        .name(signup.handle())
        .displayName(signup.displayName())
        .build();
  }
}
