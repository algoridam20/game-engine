package com.algoridam.games.mechakuchago.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.mechakuchago.dto.LockMoveRequest;
import com.algoridam.games.mechakuchago.game.MechakuchaGoMatch;
import com.algoridam.games.mechakuchago.game.dto.PlayerView;
import com.algoridam.games.mechakuchago.rules.Color;
import com.algoridam.games.mechakuchago.rules.Direction;
import com.algoridam.games.mechakuchago.rules.Move;
import com.algoridam.games.service.debug.StompTrafficLog;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MechakuchaGoGameManager {
  private final ConcurrentHashMap<UUID, MechakuchaGoMatch> matches = new ConcurrentHashMap<>();
  private final SimpMessageSendingOperations messageTemplate;
  private final StompTrafficLog stompTrafficLog;

  public void initGame(UUID roomId, String[] players, String[] handles) {
    if (players.length != 2) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    if (matches.containsKey(roomId)) {
      return;
    }
    matches.put(
        roomId,
        new MechakuchaGoMatch(
            players[0],
            players[1],
            handleAt(handles, 0, players[0]),
            handleAt(handles, 1, players[1])));
    publishGameState(roomId);
  }

  public void destroyGame(UUID roomId) {
    matches.remove(roomId);
  }

  public void publishGameState(UUID roomId) {
    MechakuchaGoMatch match = matches.get(roomId);
    if (match == null) {
      log.info("Skipping game-state publish; room {} has not started", roomId);
      return;
    }
    publish(roomId, match);
  }

  public void lockMove(UUID roomId, String playerId, LockMoveRequest request) {
    MechakuchaGoMatch match = require(roomId);
    match.lockMove(playerId, toMove(match, playerId, request));
    publish(roomId, match);
  }

  public void nextRound(UUID roomId) {
    MechakuchaGoMatch match = require(roomId);
    match.nextRound();
    publish(roomId, match);
  }

  private void publish(UUID roomId, MechakuchaGoMatch match) {
    send(roomId, match.viewFor(match.blackId()));
    send(roomId, match.viewFor(match.whiteId()));
  }

  private void send(UUID roomId, PlayerView view) {
    String destination = "/topic/room/" + roomId + "/player/" + view.playerId();
    stompTrafficLog.recordPublish(destination, view);
    messageTemplate.convertAndSend(destination, view);
  }

  private MechakuchaGoMatch require(UUID roomId) {
    MechakuchaGoMatch match = matches.get(roomId);
    if (match == null) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
    }
    return match;
  }

  private static Move toMove(MechakuchaGoMatch match, String playerId, LockMoveRequest request) {
    if (request == null || Boolean.TRUE.equals(request.pass())) {
      return null;
    }
    if (request.direction() == null || request.index() == null || request.stop() == null) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "A move needs a direction, line, and stop");
    }
    Direction direction;
    try {
      direction = Direction.valueOf(request.direction().trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Unknown direction");
    }
    Color color = Color.valueOf(match.viewFor(playerId).color());
    return new Move(color, direction, request.index(), request.stop());
  }

  private static String handleAt(String[] handles, int index, String fallback) {
    if (handles != null
        && handles.length > index
        && handles[index] != null
        && !handles[index].isBlank()) {
      return handles[index];
    }
    return fallback;
  }
}
