package com.algoridam.games.mechakuchago.rules;

public enum Color {
  BLACK,
  WHITE;

  public String label() {
    return this == BLACK ? "Black" : "White";
  }

  public Color opponent() {
    return this == BLACK ? WHITE : BLACK;
  }
}
