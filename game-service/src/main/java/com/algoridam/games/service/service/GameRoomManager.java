package com.algoridam.games.service.service;

import static com.algoridam.games.service.model.ChatMessage.createChatMessage;
import static com.algoridam.games.service.model.MessageType.CHAT;
import static com.algoridam.games.service.model.MessageType.JOIN;
import static com.algoridam.games.service.model.MessageType.LEAVE;
import static com.algoridam.games.service.security.CurrentPlayerJwts.require;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.auth.PlayerJwtIssuer;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.common.id.UuidV7Generator;
import com.algoridam.games.service.dto.RoomDtos.CreateRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.JoinRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.RoomSessionResponse;
import com.algoridam.games.service.game.GameKind;
import com.algoridam.games.service.game.GameKindRegistry;
import com.algoridam.games.service.model.ChatMessage;
import com.algoridam.games.service.model.GameRoom;
import com.algoridam.games.service.model.RoomSeat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomManager {

  private final ConcurrentHashMap<UUID, GameRoom> rooms = new ConcurrentHashMap<>();
  private final GameKindRegistry gameKindRegistry;
  private final PlayerJwtIssuer playerJwtIssuer;
  private final SimpMessageSendingOperations messageTemplate;

  public RoomSessionResponse createRoom(CreateRoomRequest request) {
    PlayerJwtInfo jwt = require();
    String gameType = resolveGameType(request);
    GameKind kind = gameKindRegistry.require(gameType);
    UUID roomId = UuidV7Generator.next();
    GameRoom room = new GameRoom(roomId, gameType);
    room.getSeats().add(new RoomSeat(jwt.playerId(), jwt.handle(), jwt.displayName()));
    rooms.put(roomId, room);
    log.info("Created room {} for player {}", roomId, jwt.playerId());
    return issueToken(room, jwt);
  }

  public RoomSessionResponse joinRoom(JoinRoomRequest request) {
    PlayerJwtInfo jwt = require();
    UUID roomId = request.roomId();
    if (jwt.gameId() != null && !jwt.gameId().equals(roomId)) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "JWT gameId does not match room");
    }
    if (jwt.gameId() != null && hasPlayer(roomId, jwt.playerId())) {
      return issueToken(getRoom(roomId), jwt);
    }
    GameRoom room = getRoom(roomId);
    GameKind kind = gameKindRegistry.require(room.getGameType());
    synchronized (room.getSeats()) {
      if (!room.hasPlayer(jwt.playerId())) {
        if (room.getSeats().size() >= kind.maxPlayers()) {
          throw new ServiceException(ErrorCode.ROOM_FULL);
        }
        room.getSeats().add(new RoomSeat(jwt.playerId(), jwt.handle(), jwt.displayName()));
      }
    }
    return issueToken(room, jwt);
  }

  public void joinRealtime(PlayerJwtInfo jwt) {
    UUID roomId = jwt.gameId();
    GameRoom room = getRoom(roomId);
    RoomSeat seat = room.seatOf(jwt.playerId());
    if (seat == null) {
      throw new ServiceException(
          ErrorCode.DATA_NOT_FOUND, "Player is not seated in room " + roomId);
    }
    ChatMessage joined = createChatMessage(seat.displayName(), "Joined", JOIN);
    messageTemplate.convertAndSend("/topic/room/" + roomId, joined);
    GameKind kind = gameKindRegistry.require(room.getGameType());
    if (room.getSeats().size() >= kind.minPlayers()
        && room.getStarted().compareAndSet(false, true)) {
      log.info("Starting {} in room {}", room.getGameType(), roomId);
      kind.onSeatsReady(roomId, room.seatsInOrder());
    } else if (room.getStarted().get()) {
      log.info("Republishing {} state for room {}", room.getGameType(), roomId);
      kind.publishState(roomId);
    }
  }

  public void sendMessage(String message, PlayerJwtInfo jwt) {
    GameRoom room = getRoom(jwt.gameId());
    RoomSeat seat = room.seatOf(jwt.playerId());
    if (seat == null) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
    }
    ChatMessage chatMessage = createChatMessage(seat.displayName(), message, CHAT);
    room.getChatMessages().add(chatMessage);
    messageTemplate.convertAndSend("/topic/room/" + jwt.gameId(), chatMessage);
  }

  public void abandonRoom(PlayerJwtInfo jwt) {
    exitRoom(jwt.playerId(), jwt.gameId(), "Abandoned");
  }

  public void exitRoom(UUID playerId, UUID roomId) {
    exitRoom(playerId, roomId, "Left");
  }

  private void exitRoom(UUID playerId, UUID roomId, String leaveMessage) {
    GameRoom room = rooms.get(roomId);
    if (room == null) {
      return;
    }
    RoomSeat seat = room.seatOf(playerId);
    if (seat == null) {
      return;
    }
    GameKind kind = gameKindRegistry.require(room.getGameType());
    synchronized (room.getSeats()) {
      room.getSeats().removeIf(existing -> existing.playerId().equals(playerId));
    }
    kind.onPlayerLeft(roomId, playerId);
    ChatMessage left = createChatMessage(seat.displayName(), leaveMessage, LEAVE);
    messageTemplate.convertAndSend("/topic/room/" + roomId, left);
    if (room.getSeats().isEmpty()) {
      rooms.remove(roomId);
      kind.destroy(roomId);
      log.info("Room {} destroyed", roomId);
    }
  }

  GameRoom getRoomForTests(UUID roomId) {
    return rooms.get(roomId);
  }

  public List<GameRoom> listActiveRooms() {
    return List.copyOf(rooms.values());
  }

  private RoomSessionResponse issueToken(GameRoom room, PlayerJwtInfo jwt) {
    String token =
        playerJwtIssuer.generate(jwt.playerId(), jwt.handle(), jwt.displayName(), room.getRoomId());
    return new RoomSessionResponse(room.getRoomId(), token);
  }

  private GameRoom getRoom(UUID roomId) {
    GameRoom room = rooms.get(roomId);
    if (room == null) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND, "Room not found: " + roomId);
    }
    return room;
  }

  private boolean hasPlayer(UUID roomId, UUID playerId) {
    GameRoom room = rooms.get(roomId);
    return room != null && room.hasPlayer(playerId);
  }

  private String resolveGameType(CreateRoomRequest request) {
    if (request == null || request.gameType() == null || request.gameType().isBlank()) {
      return GameKindRegistry.DEFAULT_GAME_TYPE;
    }
    return request.gameType();
  }
}
