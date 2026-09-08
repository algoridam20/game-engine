package com.algoridam.games.seveneight.game;

import com.algoridam.games.service.game.GameKind;
import com.algoridam.games.service.model.RoomSeat;
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
  public void onSeatsReady(UUID roomId, List<RoomSeat> seats) {
    sevenEightGameManager.initGame(
        roomId,
        seats.stream().map(seat -> seat.playerId().toString()).toArray(String[]::new),
        seats.stream().map(SevenEightGameKind::seatHandle).toArray(String[]::new));
  }

  private static String seatHandle(RoomSeat seat) {
    if (seat.handle() != null && !seat.handle().isBlank()) {
      return seat.handle();
    }
    return seat.displayName();
  }

  @Override
  public void publishState(UUID roomId) {
    sevenEightGameManager.publishGameState(roomId);
  }

  @Override
  public void onPlayerLeft(UUID roomId, UUID playerId) {}

  @Override
  public void destroy(UUID roomId) {
    sevenEightGameManager.destroyGame(roomId);
  }
}
