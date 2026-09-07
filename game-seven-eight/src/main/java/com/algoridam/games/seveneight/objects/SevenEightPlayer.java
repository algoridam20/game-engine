package com.algoridam.games.seveneight.objects;

import static com.algoridam.games.seveneight.objects.Constants.FIVE;
import static com.algoridam.games.seveneight.objects.Constants.SEVEN;
import static com.algoridam.games.seveneight.objects.Constants.TEN;

import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.util.RandomNumberGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class SevenEightPlayer {

  private final String playerId;
  private final int totalPoints;
  private final int targetHands;
  @Setter private SevenEightCard[] handCards;
  @Setter private SevenEightCard[] openCards;
  @Setter private SevenEightCard[] closedCards;

  @Setter private List<Hand> winningHands;
  @Setter private boolean winner;

  public SevenEightPlayer(
      final String playerId,
      final int totalPoints,
      final int targetHands,
      final SevenEightDeck shuffledDeck) {
    this.playerId = playerId;
    this.totalPoints = totalPoints;
    this.targetHands = targetHands;
    this.winningHands = new ArrayList<>();

    handCards = new SevenEightCard[FIVE];
    openCards = new SevenEightCard[FIVE];
    closedCards = new SevenEightCard[FIVE];
    int start = (targetHands == SEVEN) ? 0 : 15;
    for (int i = 0; i < FIVE; i++) {
      handCards[i] = shuffledDeck.getCards().get(start + i);
      openCards[i] = shuffledDeck.getCards().get(start + FIVE + i);
      closedCards[i] = shuffledDeck.getCards().get(start + TEN + i);
      handCards[i].setCardUsable(true);
    }
  }

  public boolean isWinner() {
    return winner;
  }

  public void shuffleHandCards() {
    for (int i = 0; i <= handCards.length - 2; i++) {
      int j = RandomNumberGenerator.getRandomNumber(i + 1, handCards.length);
      SevenEightCard x = handCards[i];
      handCards[i] = handCards[j];
      handCards[j] = x;
    }
  }

  public static SevenEightPlayer getOpponentsVisibleState(
      final SevenEightPlayer opponent, final boolean isPreGame) {
    SevenEightPlayer newPlayer =
        new SevenEightPlayer(
            opponent.getPlayerId(), opponent.getTotalPoints(), opponent.getTargetHands());
    SevenEightCard[] newHandCards = new SevenEightCard[FIVE];
    SevenEightCard[] newOpenCards = new SevenEightCard[FIVE];
    SevenEightCard[] newClosedCards = new SevenEightCard[FIVE];
    Card joker = new Card(null, 1);
    for (int i = 0; i < FIVE; i++) {
      newHandCards[i] = new SevenEightCard(joker, opponent.getHandCards()[i].isCardUsed(), false);

      newOpenCards[i] =
          (isPreGame)
              ? new SevenEightCard(joker, false, false)
              : new SevenEightCard(
                  opponent.getOpenCards()[i].getCard(),
                  opponent.getOpenCards()[i].isCardUsed(),
                  false);

      newClosedCards[i] =
          (opponent.getClosedCards()[i].isCardUsable())
              ? new SevenEightCard(
                  opponent.getClosedCards()[i].getCard(),
                  opponent.getClosedCards()[i].isCardUsed(),
                  false)
              : new SevenEightCard(joker, opponent.getClosedCards()[i].isCardUsed(), false);
    }
    newPlayer.setWinningHands(opponent.getWinningHands());
    newPlayer.setWinner(opponent.getWinningHands().size() > opponent.getTargetHands());
    newPlayer.setHandCards(newHandCards);
    newPlayer.setClosedCards(newClosedCards);
    newPlayer.setOpenCards(newOpenCards);
    return newPlayer;
  }

  public SevenEightPlayer getPlayersVisibleState(final boolean isPreGame) {
    SevenEightPlayer newPlayer =
        new SevenEightPlayer(this.getPlayerId(), this.getTotalPoints(), this.getTargetHands());
    SevenEightCard[] newHandCards = new SevenEightCard[FIVE];
    SevenEightCard[] newOpenCards = new SevenEightCard[FIVE];
    SevenEightCard[] newClosedCards = new SevenEightCard[FIVE];
    Card joker = new Card(null, 1);
    for (int i = 0; i < FIVE; i++) {
      newHandCards[i] =
          new SevenEightCard(
              this.getHandCards()[i].getCard(),
              this.getHandCards()[i].isCardUsed(),
              this.getHandCards()[i].isCardUsable());
      newOpenCards[i] =
          (isPreGame)
              ? new SevenEightCard(joker, false, false)
              : new SevenEightCard(
                  this.getOpenCards()[i].getCard(),
                  this.getOpenCards()[i].isCardUsed(),
                  this.getOpenCards()[i].isCardUsable());

      newClosedCards[i] =
          (this.getClosedCards()[i].isCardUsable())
              ? new SevenEightCard(
                  this.getClosedCards()[i].getCard(), this.getClosedCards()[i].isCardUsed(), true)
              : new SevenEightCard(joker, this.getClosedCards()[i].isCardUsed(), false);
    }
    newPlayer.setWinningHands(this.getWinningHands());
    newPlayer.setWinner(this.getWinningHands().size() > this.getTargetHands());
    newPlayer.setHandCards(newHandCards);
    newPlayer.setClosedCards(newClosedCards);
    newPlayer.setOpenCards(newOpenCards);
    return newPlayer;
  }
}
