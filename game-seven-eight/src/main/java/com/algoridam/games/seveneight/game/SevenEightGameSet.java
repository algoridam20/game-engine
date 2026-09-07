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
  private int playerAScore;
  private int playerBScore;
  private final int totalRounds;
  private int currRoundNumber;
  private String currGameId;
  private SevenEightGameState currGameState;

  public SevenEightGameSet(String roomId, String playerAId, String playerBId, int totalRounds) {
    this.roomId = roomId;
    this.playerAId = playerAId;
    this.playerBId = playerBId;
    this.totalRounds = totalRounds;
    this.createAt = Instant.now();
    currRoundNumber = 1;
    currGameId = currRoundNumber + roomId;
    playerAScore = 0;
    playerBScore = 0;
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
    int myScore = playerId.equals(playerAId) ? playerAScore : playerBScore;
    int theirScore = playerId.equals(playerAId) ? playerBScore : playerAScore;
    SevenEightPlayer self =
        players[playerIndex].getPlayersVisibleState(currGameState.isPreGame());
    SevenEightPlayer opponent =
        getOpponentsVisibleState(players[opponentIndex], currGameState.isPreGame());
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

    if (!currGameState.isRoundOver()) throw new ServiceException(ErrorCode.BAD_REQUEST);

    SevenEightPlayer[] players = currGameState.getPlayers();
    int playerAIndex = (this.playerAId.equals(players[0].getPlayerId())) ? 0 : 1;
    int playerBIndex = 1 - playerAIndex;
    int playerADelta =
        players[playerAIndex].getWinningHands().size() - players[playerAIndex].getTargetHands();
    int playerBDelta =
        players[playerBIndex].getWinningHands().size() - players[playerBIndex].getTargetHands();
    playerAScore += playerADelta;
    playerBScore += playerBDelta;

    if (isGameOver()) return;
    currRoundNumber++;
    currGameId = currRoundNumber + roomId;
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
  }

  private boolean isGameOver() {
    return currGameState.isRoundOver() && totalRounds == currRoundNumber;
  }
}
