package com.algoridam.games.service.game;

import java.util.List;
import java.util.UUID;

public interface GameKind {

  String type();

  int minPlayers();

  int maxPlayers();

  void onSeatsReady(UUID roomId, List<UUID> playerIdsInSeatOrder);

  void onPlayerLeft(UUID roomId, UUID playerId);

  void destroy(UUID roomId);
}
