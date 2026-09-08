package com.algoridam.games.seveneight.objects;

import com.algoridam.games.seveneight.cards.Card;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SevenEightCard {
  private final Card card;

  @Setter
  @JsonProperty("cardUsed")
  private boolean isCardUsed;

  @Setter
  @JsonProperty("cardUsable")
  private boolean isCardUsable;

  public SevenEightCard(Card card, boolean isCardUsed, boolean isCardUsable) {
    this.card = card;
    this.isCardUsed = isCardUsed;
    this.isCardUsable = isCardUsable;
  }

  public String toString() {
    return card.toString() + "|" + (isCardUsed ? 1 : 0) + "|" + (isCardUsable ? 1 : 0);
  }
}
