package com.algoridam.games.service.game;

import com.algoridam.games.service.model.RoomSeat;
import java.util.List;
import java.util.UUID;

public interface GameKind {

  String type();

  int minPlayers();

  int maxPlayers();

  void onSeatsReady(UUID roomId, List<RoomSeat> seats);

  default void publishState(UUID roomId) {}

  void onPlayerLeft(UUID roomId, UUID playerId);

  void destroy(UUID roomId);
}
