package com.algoridam.games.player.repository;

import com.algoridam.games.player.entity.PlayerEntity;
import com.algoridam.games.player.entity.PlayerPasskeyEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPasskeyRepository extends JpaRepository<PlayerPasskeyEntity, UUID> {
  Optional<PlayerPasskeyEntity> findByCredentialId(String credentialId);

  List<PlayerPasskeyEntity> findByPlayer(PlayerEntity player);

  List<PlayerPasskeyEntity> findByPlayerId(UUID playerId);

  long countByPlayer(PlayerEntity player);

  boolean existsByPlayerAndLabel(PlayerEntity player, String label);

  Optional<PlayerPasskeyEntity> findByIdAndPlayer(UUID id, PlayerEntity player);
}
