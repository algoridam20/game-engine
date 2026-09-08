package com.algoridam.games.seveneight.game;

import static com.algoridam.games.seveneight.objects.SevenEightPlayer.getOpponentsVisibleState;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import com.algoridam.games.seveneight.game.dto.PlayersGameState;
import com.algoridam.games.seveneight.objects.SevenEightGameState;
import com.algoridam.games.seveneight.objects.SevenEightPlayer;
import java.time.Instant;
import lombok.Getter;

@Getter
public class SevenEightGameSet {

  private String hashCode;
  private final Instant createAt;
  private final String roomId;
  private final String playerAId;
  private final String playerBId;
  private final String playerAHandle;
  private final String playerBHandle;
  private int playerAScore;
  private int playerBScore;
  private boolean roundScoreApplied;
  private final int totalRounds;
  private int currRoundNumber;
  private String currGameId;
  private SevenEightGameState currGameState;

  public SevenEightGameSet(
      String roomId,
      String playerAId,
      String playerBId,
      String playerAHandle,
      String playerBHandle,
      int totalRounds) {
    this.roomId = roomId;
    this.playerAId = playerAId;
    this.playerBId = playerBId;
    this.playerAHandle = playerAHandle;
    this.playerBHandle = playerBHandle;
    this.totalRounds = totalRounds;
    this.createAt = Instant.now();
    currRoundNumber = 1;
    currGameId = currRoundNumber + roomId;
    playerAScore = 0;
    playerBScore = 0;
    roundScoreApplied = false;
    currGameState =
        new SevenEightGameState(currGameId, playerAId, playerBId, playerAScore, playerBScore);
  }

  public PlayersGameState getPlayerState(final String playerId) throws ServiceException {
    if (!playerId.equals(playerAId) && !playerId.equals(playerBId)) {
      throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
    }
    SevenEightPlayer[] players = currGameState.getPlayers();
    int playerIndex = (playerId.equals(players[0].getPlayerId())) ? 0 : 1;
    int opponentIndex = 1 - playerIndex;
    boolean gameOver = isGameOver();
    int[] scores = scoresForView();
    int myScore = playerId.equals(playerAId) ? scores[0] : scores[1];
    int theirScore = playerId.equals(playerAId) ? scores[1] : scores[0];
    SevenEightPlayer self = players[playerIndex].getPlayersVisibleState(currGameState.isPreGame());
    SevenEightPlayer opponent =
        getOpponentsVisibleState(players[opponentIndex], currGameState.isPreGame());
    self.setHandle(handleFor(playerId));
    opponent.setHandle(handleFor(players[opponentIndex].getPlayerId()));
    if (gameOver) {
      self.setWinner(myScore > theirScore);
      opponent.setWinner(theirScore > myScore);
    }
    return PlayersGameState.builder()
        .playerState(self)
        .opponentsState(opponent)
        .totalRounds(this.totalRounds)
        .roundNo(this.currRoundNumber)
        .score(myScore)
        .opponentsScore(theirScore)
        .isPlayersTurn(!gameOver && currGameState.isPlayersTurn(playerId))
        .trumpSuit(currGameState.getTrumpSuit())
        .turnStartPlayedCard(currGameState.getTurnStartPlayedCard())
        .isPreGame(currGameState.isPreGame())
        .isRoundOver(currGameState.isRoundOver())
        .isGameOver(gameOver)
        .createdAt(this.createAt)
        .build();
  }

  public void setTrump(final String playerId, final Suit trumpSuit) {
    currGameState.setTrumpSuit(playerId, trumpSuit);
  }

  public void nextRound() {
    if (!currGameState.isRoundOver()) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    SevenEightPlayer[] players = currGameState.getPlayers();
    int playerAIndex = this.playerAId.equals(players[0].getPlayerId()) ? 0 : 1;
    applyRoundScoreIfNeeded();
    if (isGameOver()) {
      return;
    }
    currRoundNumber++;
    currGameId = currRoundNumber + roomId;
    roundScoreApplied = false;
    if (playerAIndex == 0) {
      currGameState =
          new SevenEightGameState(currGameId, playerBId, playerAId, playerBScore, playerAScore);
    } else {
      currGameState =
          new SevenEightGameState(currGameId, playerAId, playerBId, playerAScore, playerBScore);
    }
  }

  public void playCard(String playerId, Card card) {
    if (isGameOver()) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    currGameState.playCard(playerId, card);
    if (currGameState.isRoundOver()) {
      applyRoundScoreIfNeeded();
    }
  }

  private int[] scoresForView() {
    int[] deltas = uncommittedRoundDeltas();
    return new int[] {playerAScore + deltas[0], playerBScore + deltas[1]};
  }

  private void applyRoundScoreIfNeeded() {
    if (roundScoreApplied || !currGameState.isRoundOver()) {
      return;
    }
    int[] deltas = uncommittedRoundDeltas();
    playerAScore += deltas[0];
    playerBScore += deltas[1];
    roundScoreApplied = true;
  }

  private int[] uncommittedRoundDeltas() {
    if (!currGameState.isRoundOver() || roundScoreApplied) {
      return new int[] {0, 0};
    }
    SevenEightPlayer[] players = currGameState.getPlayers();
    int playerAIndex = this.playerAId.equals(players[0].getPlayerId()) ? 0 : 1;
    int playerBIndex = 1 - playerAIndex;
    int playerADelta =
        players[playerAIndex].getWinningHands().size() - players[playerAIndex].getTargetHands();
    int playerBDelta =
        players[playerBIndex].getWinningHands().size() - players[playerBIndex].getTargetHands();
    return new int[] {playerADelta, playerBDelta};
  }

  private String handleFor(String playerId) {
    if (playerId.equals(playerAId)) {
      return playerAHandle;
    }
    return playerBHandle;
  }

  private boolean isGameOver() {
    return currGameState.isRoundOver() && totalRounds == currRoundNumber;
  }
}
