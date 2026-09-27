package com.algoridam.games.mechakuchago.game;

import com.algoridam.games.mechakuchago.service.MechakuchaGoGameManager;
import com.algoridam.games.service.game.GameKind;
import com.algoridam.games.service.model.RoomSeat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MechakuchaGoGameKind implements GameKind {

  public static final String TYPE = "MECHAKUCHA_GO";

  private final MechakuchaGoGameManager gameManager;

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
    gameManager.initGame(
        roomId,
        seats.stream().map(seat -> seat.playerId().toString()).toArray(String[]::new),
        seats.stream().map(MechakuchaGoGameKind::seatHandle).toArray(String[]::new));
  }

  private static String seatHandle(RoomSeat seat) {
    if (seat.handle() != null && !seat.handle().isBlank()) {
      return seat.handle();
    }
    return seat.displayName();
  }

  @Override
  public void publishState(UUID roomId) {
    gameManager.publishGameState(roomId);
  }

  @Override
  public void onPlayerLeft(UUID roomId, UUID playerId) {}

  @Override
  public void destroy(UUID roomId) {
    gameManager.destroyGame(roomId);
  }
}
