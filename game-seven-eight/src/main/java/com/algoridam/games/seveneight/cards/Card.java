package com.algoridam.games.seveneight.cards;

public record Card(Suit suit, int id) {
  public Card {
    assert id > 0 && id < 15 : "Invalid Card";
  }

  public String toString() {
    String suitDisplay = suit == null ? "" : suit.getDisplay();
    return switch (id) {
      case 1 -> suitDisplay + "Joker";
      case 14 -> suitDisplay + "A";
      case 13 -> suitDisplay + "K";
      case 12 -> suitDisplay + "Q";
      case 11 -> suitDisplay + "J";
      default -> suitDisplay + id;
    };
  }

  public boolean equals(Card card) {
    return this.suit == card.suit && this.id == card.id;
  }
}
