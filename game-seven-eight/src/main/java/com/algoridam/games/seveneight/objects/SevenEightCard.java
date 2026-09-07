package com.algoridam.games.seveneight.objects;

import com.algoridam.games.seveneight.cards.Card;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SevenEightCard {
  private final Card card;

  @Setter private boolean isCardUsed;
  @Setter private boolean isCardUsable;

  public SevenEightCard(Card card, boolean isCardUsed, boolean isCardUsable) {
    this.card = card;
    this.isCardUsed = isCardUsed;
    this.isCardUsable = isCardUsable;
  }

  public String toString() {
    return card.toString() + "|" + (isCardUsed ? 1 : 0) + "|" + (isCardUsable ? 1 : 0);
  }
}
