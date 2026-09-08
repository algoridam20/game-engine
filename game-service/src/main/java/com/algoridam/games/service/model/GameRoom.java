package com.algoridam.games.service.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

@Getter
public class GameRoom {
  private final UUID roomId;
  private final String gameType;
  private final Instant createdAt;
  private final List<RoomSeat> seats;
  private final List<ChatMessage> chatMessages;
  private final AtomicBoolean started;

  public GameRoom(UUID roomId, String gameType) {
    this.roomId = roomId;
    this.gameType = gameType;
    this.createdAt = Instant.now();
    this.seats = Collections.synchronizedList(new ArrayList<>());
    this.chatMessages = Collections.synchronizedList(new ArrayList<>());
    this.started = new AtomicBoolean(false);
  }

  public boolean hasPlayer(UUID playerId) {
    synchronized (seats) {
      return seats.stream().anyMatch(seat -> seat.playerId().equals(playerId));
    }
  }

  public RoomSeat seatOf(UUID playerId) {
    synchronized (seats) {
      return seats.stream()
          .filter(seat -> seat.playerId().equals(playerId))
          .findFirst()
          .orElse(null);
    }
  }

  public List<RoomSeat> seatsInOrder() {
    synchronized (seats) {
      return List.copyOf(seats);
    }
  }

  public List<UUID> playerIdsInSeatOrder() {
    synchronized (seats) {
      return seats.stream().map(RoomSeat::playerId).toList();
    }
  }
}
