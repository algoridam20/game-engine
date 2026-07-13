package com.algoridam.games.player.dao;

import com.algoridam.games.player.entity.PlayerEntity;
import com.algoridam.games.player.entity.PlayerPasskeyEntity;
import com.algoridam.games.player.repository.PlayerPasskeyRepository;
import com.algoridam.games.player.repository.PlayerRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PlayerUserCredentialDao implements UserCredentialRepository {

  private final PlayerRepository players;
  private final PlayerPasskeyRepository passkeys;

  @Override
  @Transactional
  public void delete(Bytes credentialId) {
    passkeys.findByCredentialId(credentialId.toBase64UrlString()).ifPresent(passkeys::delete);
  }

  @Override
  @Transactional
  public void save(CredentialRecord credentialRecord) {
    String credentialId = credentialRecord.getCredentialId().toBase64UrlString();
    PlayerEntity player =
        players
            .findById(playerIdFrom(credentialRecord.getUserEntityUserId()))
            .orElseThrow(() -> new IllegalArgumentException("Player does not exist"));
    PlayerPasskeyEntity passkey =
        passkeys.findByCredentialId(credentialId).orElseGet(() -> new PlayerPasskeyEntity(player));
    passkey.updateFrom(credentialRecord);
    passkeys.saveAndFlush(passkey);
  }

  @Override
  public CredentialRecord findByCredentialId(Bytes credentialId) {
    return passkeys
        .findByCredentialId(credentialId.toBase64UrlString())
        .map(PlayerPasskeyEntity::toCredentialRecord)
        .orElse(null);
  }

  @Override
  public List<CredentialRecord> findByUserId(Bytes userId) {
    return passkeys.findByPlayerId(playerIdFrom(userId)).stream()
        .map(PlayerPasskeyEntity::toCredentialRecord)
        .toList();
  }

  private UUID playerIdFrom(Bytes userId) {
    return UUID.fromString(new String(userId.getBytes(), StandardCharsets.UTF_8));
  }
}
