package com.algoridam.games.seveneight.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import org.junit.jupiter.api.Test;

class SevenEightGameStateTest {

  @Test
  void startTurn_PlayedByIncorrectPlayer() {
    SevenEightGameState state = getSevenEightGameState();
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> state.startTurn("player1", new Card(Suit.SPADE, 9)));
    assertEquals("Invalid Player to start Turn", exception.getMessage());
  }

  @Test
  void startTurn_IncorrectCardThrown() {
    SevenEightGameState state = getSevenEightGameState();
    state.startTurn("player0", new Card(Suit.CLUBS, 10));
    state.endTurn("player1", new Card(Suit.CLUBS, 12));
    state.startTurn("player1", new Card(Suit.HEARTS, 14));
    state.endTurn("player0", new Card(Suit.HEARTS, 9));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> state.startTurn("player1", new Card(Suit.CLUBS, 12)));
    assertEquals("Invalid Card Thrown, Cannot find in usable cards", exception.getMessage());
  }

  @Test
  void endTurn_IncorrectCardThrown_SameSuitCardIsPreset() {
    SevenEightGameState state = getSevenEightGameState();
    state.startTurn("player0", new Card(Suit.CLUBS, 10));
    state.endTurn("player1", new Card(Suit.CLUBS, 12));
    state.startTurn("player1", new Card(Suit.HEARTS, 14));
    state.endTurn("player0", new Card(Suit.HEARTS, 9));
    state.startTurn("player1", new Card(Suit.HEARTS, 13));
    state.endTurn("player0", new Card(Suit.HEARTS, 11));
    state.startTurn("player1", new Card(Suit.CLUBS, 11));
    state.endTurn("player0", new Card(Suit.CLUBS, 14));
    state.startTurn("player0", new Card(Suit.SPADE, 7));
    state.endTurn("player1", new Card(Suit.SPADE, 9));
    state.startTurn("player1", new Card(Suit.SPADE, 14));
    state.endTurn("player0", new Card(Suit.SPADE, 12));
    state.startTurn("player1", new Card(Suit.SPADE, 11));
    state.endTurn("player0", new Card(Suit.SPADE, 13));
    state.startTurn("player0", new Card(Suit.HEARTS, 7));
    state.endTurn("player1", new Card(Suit.HEARTS, 8));
    state.startTurn("player1", new Card(Suit.DIAMOND, 11));
    state.endTurn("player0", new Card(Suit.DIAMOND, 12));
    state.startTurn("player0", new Card(Suit.DIAMOND, 9));
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> state.endTurn("player1", new Card(Suit.HEARTS, 10)));
    assertEquals("Invalid Card thrown, Card of Same Suit is present", exception.getMessage());
  }

  @Test
  void endTurn_IncorrectCardThrownSameSuitCardIsPreset2() {
    SevenEightGameState state = getSevenEightGameState();
    state.startTurn("player0", new Card(Suit.CLUBS, 10));
    state.endTurn("player1", new Card(Suit.CLUBS, 12));
    state.startTurn("player1", new Card(Suit.HEARTS, 14));
    state.endTurn("player0", new Card(Suit.HEARTS, 9));
    state.startTurn("player1", new Card(Suit.HEARTS, 13));
    state.endTurn("player0", new Card(Suit.HEARTS, 11));
    state.startTurn("player1", new Card(Suit.CLUBS, 11));
    state.endTurn("player0", new Card(Suit.CLUBS, 14));
    state.startTurn("player0", new Card(Suit.SPADE, 7));
    state.endTurn("player1", new Card(Suit.SPADE, 9));
    state.startTurn("player1", new Card(Suit.SPADE, 14));
    state.endTurn("player0", new Card(Suit.SPADE, 12));
    state.startTurn("player1", new Card(Suit.SPADE, 11));
    state.endTurn("player0", new Card(Suit.SPADE, 13));
    state.startTurn("player0", new Card(Suit.HEARTS, 7));
    state.endTurn("player1", new Card(Suit.HEARTS, 8));
    state.startTurn("player1", new Card(Suit.DIAMOND, 11));
    state.endTurn("player0", new Card(Suit.DIAMOND, 12));
    state.startTurn("player0", new Card(Suit.DIAMOND, 9));
    state.endTurn("player1", new Card(Suit.DIAMOND, 14));
    state.startTurn("player1", new Card(Suit.CLUBS, 13));
    state.endTurn("player0", new Card(Suit.CLUBS, 8));
    state.startTurn("player1", new Card(Suit.CLUBS, 9));
    state.endTurn("player0", new Card(Suit.HEARTS, 12));
    state.startTurn("player0", new Card(Suit.DIAMOND, 13));
    state.endTurn("player1", new Card(Suit.DIAMOND, 10));
    state.startTurn("player0", new Card(Suit.DIAMOND, 8));
    state.endTurn("player1", new Card(Suit.HEARTS, 10));
    state.startTurn("player1", new Card(Suit.SPADE, 10));
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> state.endTurn("player0", new Card(Suit.CLUBS, 8)));
    assertEquals("Invalid Card thrown, Card of Same Suit is present", exception.getMessage());
  }

  @Test
  void completeSuccessfulGame() {
    SevenEightGameState state = getSevenEightGameState();
    state.startTurn("player0", new Card(Suit.CLUBS, 10));
    state.endTurn("player1", new Card(Suit.CLUBS, 12));
    state.startTurn("player1", new Card(Suit.HEARTS, 14));
    state.endTurn("player0", new Card(Suit.HEARTS, 9));
    state.startTurn("player1", new Card(Suit.HEARTS, 13));
    state.endTurn("player0", new Card(Suit.HEARTS, 11));
    state.startTurn("player1", new Card(Suit.CLUBS, 11));
    state.endTurn("player0", new Card(Suit.CLUBS, 14));
    state.startTurn("player0", new Card(Suit.SPADE, 7));
    state.endTurn("player1", new Card(Suit.SPADE, 9));
    state.startTurn("player1", new Card(Suit.SPADE, 14));
    state.endTurn("player0", new Card(Suit.SPADE, 12));
    state.startTurn("player1", new Card(Suit.SPADE, 11));
    state.endTurn("player0", new Card(Suit.SPADE, 13));
    state.startTurn("player0", new Card(Suit.HEARTS, 7));
    state.endTurn("player1", new Card(Suit.HEARTS, 8));
    state.startTurn("player1", new Card(Suit.DIAMOND, 11));
    state.endTurn("player0", new Card(Suit.DIAMOND, 12));
    state.startTurn("player0", new Card(Suit.DIAMOND, 9));
    state.endTurn("player1", new Card(Suit.DIAMOND, 14));
    state.startTurn("player1", new Card(Suit.CLUBS, 13));
    state.endTurn("player0", new Card(Suit.CLUBS, 8));
    state.startTurn("player1", new Card(Suit.CLUBS, 9));
    state.endTurn("player0", new Card(Suit.HEARTS, 12));
    state.startTurn("player0", new Card(Suit.DIAMOND, 13));
    state.endTurn("player1", new Card(Suit.DIAMOND, 10));
    state.startTurn("player0", new Card(Suit.DIAMOND, 8));
    state.endTurn("player1", new Card(Suit.HEARTS, 10));
    state.startTurn("player1", new Card(Suit.SPADE, 10));
    state.endTurn("player0", new Card(Suit.SPADE, 8));
    assertTrue(state.isRoundOver());
  }

  private SevenEightGameState getSevenEightGameState() {
    String gameId = "gameId";
    String player0Id = "player0";
    String player1Id = "player1";
    int player0Score = 0;
    int player1Score = 0;
    SevenEightPlayer player0 =
        new SevenEightPlayer(player0Id, player0Score, 8, new SevenEightDeck());
    SevenEightPlayer player1 =
        new SevenEightPlayer(player1Id, player1Score, 7, new SevenEightDeck());
    player0.setHandCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.DIAMOND, 8), false, true),
          new SevenEightCard(new Card(Suit.CLUBS, 10), false, true),
          new SevenEightCard(new Card(Suit.HEARTS, 11), false, true),
          new SevenEightCard(new Card(Suit.HEARTS, 12), false, true),
          new SevenEightCard(new Card(Suit.DIAMOND, 13), false, true)
        });
    player1.setHandCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.SPADE, 9), false, true),
          new SevenEightCard(new Card(Suit.CLUBS, 11), false, true),
          new SevenEightCard(new Card(Suit.SPADE, 11), false, true),
          new SevenEightCard(new Card(Suit.HEARTS, 13), false, true),
          new SevenEightCard(new Card(Suit.HEARTS, 8), false, true)
        });
    player0.setClosedCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.SPADE, 8), false, false),
          new SevenEightCard(new Card(Suit.HEARTS, 7), false, false),
          new SevenEightCard(new Card(Suit.DIAMOND, 9), false, false),
          new SevenEightCard(new Card(Suit.SPADE, 12), false, false),
          new SevenEightCard(new Card(Suit.CLUBS, 14), false, false)
        });
    player1.setClosedCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.HEARTS, 10), false, false),
          new SevenEightCard(new Card(Suit.DIAMOND, 10), false, false),
          new SevenEightCard(new Card(Suit.DIAMOND, 14), false, false),
          new SevenEightCard(new Card(Suit.SPADE, 10), false, false),
          new SevenEightCard(new Card(Suit.SPADE, 14), false, false)
        });
    player0.setOpenCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.CLUBS, 8), false, false),
          new SevenEightCard(new Card(Suit.SPADE, 13), false, false),
          new SevenEightCard(new Card(Suit.DIAMOND, 12), false, false),
          new SevenEightCard(new Card(Suit.SPADE, 7), false, false),
          new SevenEightCard(new Card(Suit.HEARTS, 9), false, false)
        });
    player1.setOpenCards(
        new SevenEightCard[] {
          new SevenEightCard(new Card(Suit.DIAMOND, 11), false, false),
          new SevenEightCard(new Card(Suit.CLUBS, 13), false, false),
          new SevenEightCard(new Card(Suit.HEARTS, 14), false, false),
          new SevenEightCard(new Card(Suit.CLUBS, 9), false, false),
          new SevenEightCard(new Card(Suit.CLUBS, 12), false, false)
        });
    SevenEightGameState state =
        new SevenEightGameState(gameId, player0Id, player1Id, player0Score, player1Score);
    state.setPlayers(new SevenEightPlayer[] {player0, player1});
    state.setTrumpSuit(player0Id, Suit.HEARTS);
    return state;
  }
}
