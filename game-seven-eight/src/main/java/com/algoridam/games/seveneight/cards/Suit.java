package com.algoridam.games.seveneight.cards;

import static com.algoridam.games.seveneight.cards.CardColor.BLACK;
import static com.algoridam.games.seveneight.cards.CardColor.RED;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Suit {
  CLUBS(BLACK, "♣"),
  DIAMOND(RED, "♦"),
  SPADE(BLACK, "♠"),
  HEARTS(RED, "♥");

  private final CardColor cardColor;
  private final String display;

  public static List<Suit> allSuits() {
    return List.of(SPADE, HEARTS, DIAMOND, CLUBS);
  }
}
