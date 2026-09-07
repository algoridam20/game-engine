package com.algoridam.games.seveneight.game;

import com.algoridam.games.service.game.GameKind;
import com.algoridam.games.seveneight.service.SevenEightGameManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SevenEightGameKind implements GameKind {

  public static final String TYPE = "SEVEN_EIGHT";

  private final SevenEightGameManager sevenEightGameManager;

  @Override
  public String type() {
    return TYPE;
  }

  @Override
  public int minPlayers() {
    return 2;
  }

  @Override
  public int maxPlayers() {
    return 2;
  }

  @Override
  public void onSeatsReady(UUID roomId, List<UUID> playerIdsInSeatOrder) {
    sevenEightGameManager.initGame(
        roomId, playerIdsInSeatOrder.stream().map(UUID::toString).toArray(String[]::new));
  }

  @Override
  public void onPlayerLeft(UUID roomId, UUID playerId) {}

  @Override
  public void destroy(UUID roomId) {
    sevenEightGameManager.destroyGame(roomId);
  }
}
