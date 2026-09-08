package com.algoridam.games.seveneight.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import com.algoridam.games.seveneight.game.dto.PlayersGameState;
import com.algoridam.games.seveneight.objects.SevenEightCard;
import com.algoridam.games.seveneight.objects.SevenEightDeck;
import com.algoridam.games.seveneight.objects.SevenEightGameState;
import com.algoridam.games.seveneight.objects.SevenEightPlayer;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class SevenEightGameSetTest {

  @Test
  void getPlayerState_includesLastRoundScoreBeforeNextRound() throws Exception {
    SevenEightGameSet gameSet =
        new SevenEightGameSet("room", "player0", "player1", "alice", "bob", 1);
    SevenEightGameState completed = completedRound();
    Field field = SevenEightGameSet.class.getDeclaredField("currGameState");
    field.setAccessible(true);
    field.set(gameSet, completed);

    PlayersGameState view = gameSet.getPlayerState("player0");
    SevenEightPlayer[] players = completed.getPlayers();
    int expectedScore = players[0].getWinningHands().size() - players[0].getTargetHands();
    int expectedOpponentScore = players[1].getWinningHands().size() - players[1].getTargetHands();

    assertTrue(view.isGameOver());
    assertEquals(expectedScore, view.getScore());
    assertEquals(expectedOpponentScore, view.getOpponentsScore());
    assertEquals(expectedScore > expectedOpponentScore, view.getPlayerState().isWinner());
  }

  private static SevenEightGameState completedRound() {
    SevenEightGameState state = seededState();
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
    return state;
  }

  private static SevenEightGameState seededState() {
    SevenEightPlayer player0 = new SevenEightPlayer("player0", 0, 8, new SevenEightDeck());
    SevenEightPlayer player1 = new SevenEightPlayer("player1", 0, 7, new SevenEightDeck());
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
    SevenEightGameState state = new SevenEightGameState("gameId", "player0", "player1", 0, 0);
    state.setPlayers(new SevenEightPlayer[] {player0, player1});
    state.setTrumpSuit("player0", Suit.HEARTS);
    return state;
  }
}
