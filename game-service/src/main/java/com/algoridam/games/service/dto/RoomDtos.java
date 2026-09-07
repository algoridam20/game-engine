package com.algoridam.games.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class RoomDtos {

  private RoomDtos() {}

  public record CreateRoomRequest(String gameType) {}

  public record JoinRoomRequest(@NotNull UUID roomId) {}

  public record RoomSessionResponse(UUID roomId, String token) {}
}
