package com.algoridam.games.seveneight.objects;

import com.algoridam.games.seveneight.cards.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Hand {
  private Card turnStartCard;
  private Card turnEndCard;
  private int turnNumber;

  public String toString() {
    return "Turn: "
        + turnNumber
        + " Turn Start Card: "
        + turnStartCard
        + " Turn End Card: "
        + turnEndCard;
  }
}
