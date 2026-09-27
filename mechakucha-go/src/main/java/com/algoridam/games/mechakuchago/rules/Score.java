package com.algoridam.games.mechakuchago.rules;

public record Score(int blackLines, int whiteLines, Color winner, boolean draw) {

  public boolean gameOver() {
    return winner != null || draw;
  }

  public int lines(Color color) {
    return color == Color.BLACK ? blackLines : whiteLines;
  }
}
