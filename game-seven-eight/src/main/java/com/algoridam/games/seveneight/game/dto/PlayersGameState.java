package com.algoridam.games.seveneight.game.dto;

import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import com.algoridam.games.seveneight.objects.SevenEightPlayer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude
@AllArgsConstructor
public class PlayersGameState {
  private Instant createdAt;

  @JsonProperty("preGame")
  private boolean isPreGame;

  @JsonProperty("gameOver")
  private boolean isGameOver;

  @JsonProperty("roundOver")
  private boolean isRoundOver;

  private Suit trumpSuit;
  private Card turnStartPlayedCard;
  private int roundNo;
  private int totalRounds;
  private int score;
  private int opponentsScore;

  @JsonProperty("playersTurn")
  private boolean isPlayersTurn;

  private SevenEightPlayer playerState;
  private SevenEightPlayer opponentsState;
}
