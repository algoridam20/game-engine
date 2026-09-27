package com.algoridam.games.mechakuchago.rules;

public record Cell(int row, int column) {

  public String key() {
    return row + "," + column;
  }
}
