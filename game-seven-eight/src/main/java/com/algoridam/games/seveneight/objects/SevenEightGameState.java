package com.algoridam.games.seveneight.objects;

import static com.algoridam.games.seveneight.objects.Constants.EIGHT;
import static com.algoridam.games.seveneight.objects.Constants.FIVE;
import static com.algoridam.games.seveneight.objects.Constants.SEVEN;
import static com.algoridam.games.seveneight.objects.Constants.TOTAL_HANDS;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SevenEightGameState {

  private final String gameId;
  @Setter private SevenEightPlayer[] players;
  private final SevenEightDeck deck;
  private Suit trumpSuit;
  private int startingTurnPlayerIndex;
  private Card turnStartPlayedCard;
  private int actionCount;

  public SevenEightGameState(
      final String gameId,
      final String player0Id,
      final String player1Id,
      final int player0Score,
      final int player1Score) {
    this.gameId = gameId;
    deck = new SevenEightDeck();
    players = new SevenEightPlayer[2];
    deck.shuffle();
    deck.shuffle();
    players[0] = new SevenEightPlayer(player0Id, player0Score, EIGHT, deck);
    players[1] = new SevenEightPlayer(player1Id, player1Score, SEVEN, deck);
    startingTurnPlayerIndex = 0;
    actionCount = 0;
  }

  public void setTrumpSuit(final String playerId, final Suit trumpSuit) {
    if (!this.isPreGame()) throw new RuntimeException("Cannot select Trump now");
    if (!players[0].getPlayerId().equals(playerId))
      throw new RuntimeException("Invalid Player to choose Trump Suit");
    if (this.trumpSuit == null) {
      this.trumpSuit = trumpSuit;
      for (SevenEightPlayer player : players) {
        for (SevenEightCard card : player.getOpenCards()) {
          card.setCardUsable(true);
        }
      }
    }
  }

  public void startTurn(final String playerId, final Card card) {
    if (!playerId.equals(players[startingTurnPlayerIndex].getPlayerId()))
      throw new RuntimeException("Invalid Player to start Turn");
    assert startingTurnPlayerIndex == 0 || startingTurnPlayerIndex == 1
        : "Cannot have more than 2 players playing";
    if (turnStartPlayedCard != null) throw new RuntimeException("Turn start already played");

    SevenEightCard[] handCards = players[startingTurnPlayerIndex].getHandCards();
    SevenEightCard[] openCards = players[startingTurnPlayerIndex].getOpenCards();
    SevenEightCard[] closedCards = players[startingTurnPlayerIndex].getClosedCards();
    int indexInHandCard = findCard(handCards, card);
    if (indexInHandCard != -1) {
      handCards[indexInHandCard].setCardUsed(true);
      turnStartPlayedCard = card;
      actionCount++;
      return;
    }
    int indexInOpenCard = findCard(openCards, card);
    if (indexInOpenCard != -1) {
      openCards[indexInOpenCard].setCardUsed(true);
      closedCards[indexInOpenCard].setCardUsable(true);
      turnStartPlayedCard = card;
      actionCount++;
      return;
    }
    int indexInClosedCard = findCard(closedCards, card);
    if (indexInClosedCard != -1) {
      closedCards[indexInClosedCard].setCardUsed(true);
      turnStartPlayedCard = card;
      actionCount++;
      return;
    }
    throw new RuntimeException("Invalid Card Thrown, Cannot find in usable cards");
  }

  public void endTurn(final String playerId, final Card card) {
    if (playerId.equals(players[startingTurnPlayerIndex].getPlayerId()))
      throw new RuntimeException("Invalid Player to end Turn");
    if (turnStartPlayedCard == null)
      throw new RuntimeException("Cannot play endTurn before startTurn");
    assert startingTurnPlayerIndex == 0 || startingTurnPlayerIndex == 1
        : "Cannot have more than 2 players playing";

    int endingTurnPlayerIndex = getEndingTurnPlayerIndex();
    SevenEightCard[] handCards = players[endingTurnPlayerIndex].getHandCards();
    SevenEightCard[] openCards = players[endingTurnPlayerIndex].getOpenCards();
    SevenEightCard[] closedCards = players[endingTurnPlayerIndex].getClosedCards();
    Hand winningHand = new Hand(turnStartPlayedCard, card, actionCount / 2);
    if (card.suit() != turnStartPlayedCard.suit() && isTurnStartSuitCardPresentForEndTurnPlayer()) {
      throw new RuntimeException("Invalid Card thrown, Card of Same Suit is present");
    }
    int indexInHandCards = findCard(handCards, card);
    if (indexInHandCards != -1) {
      handCards[indexInHandCards].setCardUsed(true);
      setTurnEndWinningHand(card, winningHand);
      return;
    }
    int indexInOpenCards = findCard(openCards, card);
    if (indexInOpenCards != -1) {
      openCards[indexInOpenCards].setCardUsed(true);
      closedCards[indexInOpenCards].setCardUsable(true);
      setTurnEndWinningHand(card, winningHand);
      return;
    }
    int indexInClosedCards = findCard(closedCards, card);
    if (indexInClosedCards != -1) {
      closedCards[indexInClosedCards].setCardUsed(true);
      setTurnEndWinningHand(card, winningHand);
      return;
    }
    throw new RuntimeException("Invalid Card Thrown, Cannot find in usable cards");
  }

  public void playCard(final String playerId, final Card card) {
    if (!this.isPlayersTurn(playerId)) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    if (this.isTurnStart()) {
      this.startTurn(playerId, card);
    } else {
      this.endTurn(playerId, card);
    }
  }

  public boolean isRoundOver() {
    return (players[0].getWinningHands().size() + players[1].getWinningHands().size())
        == TOTAL_HANDS;
  }

  public boolean isPreGame() {
    return trumpSuit == null;
  }

  public boolean isPlayersTurn(final String playerId) {
    if (turnStartPlayedCard == null)
      return playerId.equals(players[startingTurnPlayerIndex].getPlayerId());

    return playerId.equals(players[getEndingTurnPlayerIndex()].getPlayerId());
  }

  public boolean isTurnStart() {
    return turnStartPlayedCard == null;
  }

  private void setTurnEndWinningHand(Card card, Hand winningHand) {
    int endingTurnPlayerIndex = getEndingTurnPlayerIndex();
    boolean isStartingTurnPlayerWinner = isFirstCardGreater(turnStartPlayedCard, card);
    if (isStartingTurnPlayerWinner) {
      players[startingTurnPlayerIndex].getWinningHands().add(winningHand);
    } else {
      players[endingTurnPlayerIndex].getWinningHands().add(winningHand);
      startingTurnPlayerIndex = endingTurnPlayerIndex;
    }
    actionCount++;
    turnStartPlayedCard = null;
  }

  private boolean isFirstCardGreater(Card a, Card b) {
    assert this.trumpSuit != null : "Trump Should be defined";
    if (a.suit() == b.suit()) {
      assert a.id() != b.id() : "Two cards cannot be same";
      return a.id() > b.id();
    }
    return b.suit() != this.getTrumpSuit();
  }

  private int findCard(SevenEightCard[] cards, Card target) {
    assert cards.length == FIVE : "Cards array cannot be more than size 5";
    for (int i = 0; i < cards.length; i++) {
      SevenEightCard card = cards[i];
      if (!card.isCardUsed() && card.isCardUsable() && target.equals(card.getCard())) return i;
    }
    return -1;
  }

  private boolean isTurnStartSuitCardPresentForEndTurnPlayer() {
    assert turnStartPlayedCard != null : "Start Turn Not Played";
    int endingTurnPlayerIndex = getEndingTurnPlayerIndex();
    SevenEightCard[] handCards = players[endingTurnPlayerIndex].getHandCards();
    SevenEightCard[] openCards = players[endingTurnPlayerIndex].getOpenCards();
    SevenEightCard[] closedCards = players[endingTurnPlayerIndex].getClosedCards();
    Optional<SevenEightCard> aValidCard =
        Stream.of(handCards, openCards, closedCards)
            .flatMap(Arrays::stream)
            .filter(
                card ->
                    !card.isCardUsed()
                        && card.isCardUsable()
                        && card.getCard().suit() == turnStartPlayedCard.suit())
            .findFirst();
    return aValidCard.isPresent();
  }

  private int getEndingTurnPlayerIndex() {
    return 1 - startingTurnPlayerIndex;
  }
}
