package com.algoridam.games.mechakuchago.rules;

public enum Direction {
  RIGHT(0, 1, Color.BLACK, true),
  LEFT(0, -1, Color.WHITE, true),
  UP(-1, 0, Color.BLACK, false),
  DOWN(1, 0, Color.WHITE, false);

  private final int rowDelta;
  private final int columnDelta;
  private final Color owner;
  private final boolean horizontal;

  Direction(int rowDelta, int columnDelta, Color owner, boolean horizontal) {
    this.rowDelta = rowDelta;
    this.columnDelta = columnDelta;
    this.owner = owner;
    this.horizontal = horizontal;
  }

  public int rowDelta() {
    return rowDelta;
  }

  public int columnDelta() {
    return columnDelta;
  }

  public Color owner() {
    return owner;
  }

  public boolean horizontal() {
    return horizontal;
  }

  public Cell step(int row, int column) {
    return new Cell(row + rowDelta, column + columnDelta);
  }
}
