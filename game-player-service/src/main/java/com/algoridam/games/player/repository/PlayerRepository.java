package com.algoridam.games.player.repository;

import com.algoridam.games.player.entity.PlayerEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
  boolean existsByHandle(String handle);

  Optional<PlayerEntity> findByHandle(String handle);
}
