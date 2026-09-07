package com.algoridam.games.seveneight.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.game.SevenEightGameSet;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SevenEightGameManager {
  private static final int TOTAL_ROUNDS = 2;

  private final ConcurrentHashMap<UUID, SevenEightGameSet> gameSessions = new ConcurrentHashMap<>();
  private final SimpMessageSendingOperations messageTemplate;

  public void initGame(final UUID roomId, final String[] players) {
    if (players.length != 2) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    if (gameSessions.containsKey(roomId)) {
      return;
    }
    SevenEightGameSet newGameSet =
        new SevenEightGameSet(roomId.toString(), players[0], players[1], TOTAL_ROUNDS);
    gameSessions.put(roomId, newGameSet);
    publishGameState(roomId);
  }

  public void destroyGame(final UUID roomId) {
    gameSessions.remove(roomId);
  }

  public void publishGameState(final UUID roomId) {
    SevenEightGameSet gameSet = getGameSet(roomId);
    Arrays.stream(gameSet.getCurrGameState().getPlayers())
        .forEach(
            player -> {
              String playerId = player.getPlayerId();
              messageTemplate.convertAndSend(
                  "/topic/room/" + roomId + "/player/" + playerId,
                  gameSet.getPlayerState(playerId));
            });
  }

  public void performActionSetTrump(final UUID roomId, final String playerId, final Card card) {
    SevenEightGameSet gameSet = getGameSet(roomId);
    gameSet.setTrump(playerId, card.suit());
    publishGameState(roomId);
  }

  public void performActionPlayCard(final UUID roomId, final String playerId, final Card card) {
    SevenEightGameSet gameSet = getGameSet(roomId);
    gameSet.playCard(playerId, card);
    publishGameState(roomId);
  }

  public void performActionInitNextRound(final UUID roomId) {
    SevenEightGameSet gameSet = getGameSet(roomId);
    gameSet.nextRound();
    publishGameState(roomId);
  }

  private SevenEightGameSet getGameSet(final UUID roomId) {
    SevenEightGameSet gameSet = gameSessions.get(roomId);
    if (gameSet == null) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
    }
    return gameSet;
  }
}
