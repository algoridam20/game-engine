package com.algoridam.games.mechakuchago.rules;

public final class Board {
  public static final int SIZE = 10;

  private final Color[][] cells = new Color[SIZE][SIZE];

  public static Board empty() {
    return new Board();
  }

  public Board copy() {
    Board copy = new Board();
    for (int row = 0; row < SIZE; row++) {
      System.arraycopy(cells[row], 0, copy.cells[row], 0, SIZE);
    }
    return copy;
  }

  public Color at(int row, int column) {
    return cells[row][column];
  }

  public void place(int row, int column, Color color) {
    cells[row][column] = color;
  }

  public static boolean inBounds(int row, int column) {
    return row >= 0 && column >= 0 && row < SIZE && column < SIZE;
  }

  public static String columnLabel(int index) {
    return String.valueOf((char) ('A' + index));
  }

  public static String cellName(int row, int column) {
    return "row " + (row + 1) + ", column " + columnLabel(column);
  }

  public static Cell axisCell(Direction direction, int index, int offset) {
    if (direction.horizontal()) {
      return new Cell(index, offset);
    }
    return new Cell(offset, index);
  }

  public boolean axisFull(Direction direction, int index) {
    for (int offset = 0; offset < SIZE; offset++) {
      Cell cell = axisCell(direction, index, offset);
      if (cells[cell.row()][cell.column()] == null) {
        return false;
      }
    }
    return true;
  }
}
