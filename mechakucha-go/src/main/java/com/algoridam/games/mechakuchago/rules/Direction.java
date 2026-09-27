package com.algoridam.games.mechakuchago.rules;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Direction {
  RIGHT(0, 1, Color.BLACK, true),
  LEFT(0, -1, Color.WHITE, true),
  UP(-1, 0, Color.BLACK, false),
  DOWN(1, 0, Color.WHITE, false);

  private final int rowDelta;
  private final int columnDelta;
  private final Color owner;
  private final boolean horizontal;

  public Cell step(int row, int column) {
    return new Cell(row + rowDelta, column + columnDelta);
  }
}
