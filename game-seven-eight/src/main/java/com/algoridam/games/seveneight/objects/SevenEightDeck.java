package com.algoridam.games.seveneight.objects;

import static com.algoridam.games.seveneight.objects.Constants.EIGHT;
import static com.algoridam.games.seveneight.objects.Constants.SEVEN;

import com.algoridam.games.seveneight.cards.Card;
import com.algoridam.games.seveneight.cards.Suit;
import com.algoridam.games.seveneight.util.RandomNumberGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class SevenEightDeck {
  private final List<SevenEightCard> cards;

  public SevenEightDeck() {
    List<SevenEightCard> cards = new ArrayList<>();
    Suit.allSuits()
        .forEach(
            suit -> {
              int start = (suit == Suit.HEARTS || suit == Suit.SPADE) ? SEVEN : EIGHT;
              for (int id = start; id <= 14; id++) {
                cards.add(new SevenEightCard(new Card(suit, id), false, false));
              }
            });
    this.cards = cards;
  }

  public void shuffle() {
    for (int i = 0; i <= cards.size() - 2; i++) {
      int j = RandomNumberGenerator.getRandomNumber(i + 1, cards.size());
      SevenEightCard x = cards.get(i);
      cards.set(i, cards.get(j));
      cards.set(j, x);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (SevenEightCard card : cards) {
      sb.append(card).append(" ");
    }
    return sb.toString();
  }
}
