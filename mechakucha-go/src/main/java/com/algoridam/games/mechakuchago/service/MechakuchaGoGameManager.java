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
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MechakuchaGoGameManager {

  private final ConcurrentHashMap<UUID, MechakuchaGoMatch> matches = new ConcurrentHashMap<>();
  private final SimpMessageSendingOperations messageTemplate;
  private final StompTrafficLog stompTrafficLog;

  public void initGame(final UUID roomId, final String[] players, final String[] handles) {
    if (players.length != 2) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    if (matches.containsKey(roomId)) {
      return;
    }
    String blackHandle = handleAt(handles, 0, players[0]);
    String whiteHandle = handleAt(handles, 1, players[1]);
    matches.put(roomId, new MechakuchaGoMatch(players[0], players[1], blackHandle, whiteHandle));
    publishGameState(roomId);
  }

  public void destroyGame(final UUID roomId) {
    matches.remove(roomId);
  }

  public void publishGameState(final UUID roomId) {
    MechakuchaGoMatch match = matches.get(roomId);
    if (match == null) {
      log.info("Skipping game-state publish; room {} has not started", roomId);
      return;
    }
    publishTo(roomId, match.viewFor(match.blackId()));
    publishTo(roomId, match.viewFor(match.whiteId()));
  }

  public void performActionLockMove(
      final UUID roomId, final String playerId, final LockMoveRequest request) {
    MechakuchaGoMatch match = getMatch(roomId);
    match.lockMove(playerId, moveFrom(match, playerId, request));
    publishGameState(roomId);
  }

  public void performActionInitNextRound(final UUID roomId) {
    MechakuchaGoMatch match = getMatch(roomId);
    match.nextRound();
    publishGameState(roomId);
  }

  private void publishTo(UUID roomId, PlayerView view) {
    String destination = "/topic/room/" + roomId + "/player/" + view.playerId();
    stompTrafficLog.recordPublish(destination, view);
    messageTemplate.convertAndSend(destination, view);
  }

  private MechakuchaGoMatch getMatch(final UUID roomId) {
    MechakuchaGoMatch match = matches.get(roomId);
    if (match == null) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
    }
    return match;
  }

  private static Move moveFrom(MechakuchaGoMatch match, String playerId, LockMoveRequest request) {
    if (request == null || Boolean.TRUE.equals(request.pass())) {
      return null;
    }
    if (request.direction() == null || request.index() == null || request.stop() == null) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "A move needs a direction, line, and stop");
    }
    Direction direction = directionFrom(request.direction());
    Color color = Color.valueOf(match.viewFor(playerId).color());
    return new Move(color, direction, request.index(), request.stop());
  }

  private static Direction directionFrom(String direction) {
    try {
      return Direction.valueOf(direction.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Unknown direction");
    }
  }

  private static String handleAt(String[] handles, int index, String fallback) {
    if (handles != null && handles.length > index && StringUtils.hasText(handles[index])) {
      return handles[index];
    }
    return fallback;
  }
}
