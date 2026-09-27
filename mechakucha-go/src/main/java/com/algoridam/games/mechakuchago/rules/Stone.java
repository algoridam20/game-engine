package com.algoridam.games.mechakuchago.rules;

public final class Stone {
  private final String id;
  private final Color color;
  private int row;
  private int column;
  private int rowDelta;
  private int columnDelta;
  private final Integer stopRow;
  private final Integer stopColumn;
  private boolean alive = true;
  private boolean moving;
  private boolean dying;

  private Stone(
      String id,
      Color color,
      int row,
      int column,
      int rowDelta,
      int columnDelta,
      Integer stopRow,
      Integer stopColumn,
      boolean moving) {
    this.id = id;
    this.color = color;
    this.row = row;
    this.column = column;
    this.rowDelta = rowDelta;
    this.columnDelta = columnDelta;
    this.stopRow = stopRow;
    this.stopColumn = stopColumn;
    this.moving = moving;
  }

  public static Stone sitting(String id, Color color, int row, int column) {
    return new Stone(id, color, row, column, 0, 0, null, null, false);
  }

  public static Stone mover(String id, Move move) {
    Direction direction = move.direction();
    int row;
    int column;
    switch (direction) {
      case RIGHT -> {
        row = move.index();
        column = -1;
      }
      case LEFT -> {
        row = move.index();
        column = Board.SIZE;
      }
      case UP -> {
        row = Board.SIZE;
        column = move.index();
      }
      default -> {
        row = -1;
        column = move.index();
      }
    }
    Integer stopRow = direction.horizontal() ? move.index() : move.stop();
    Integer stopColumn = direction.horizontal() ? move.stop() : move.index();
    return new Stone(
        id,
        move.player(),
        row,
        column,
        direction.rowDelta(),
        direction.columnDelta(),
        stopRow,
        stopColumn,
        true);
  }

  public String id() {
    return id;
  }

  public Color color() {
    return color;
  }

  public int row() {
    return row;
  }

  public int column() {
    return column;
  }

  public int rowDelta() {
    return rowDelta;
  }

  public int columnDelta() {
    return columnDelta;
  }

  public boolean alive() {
    return alive;
  }

  public boolean moving() {
    return moving;
  }

  public boolean dying() {
    return dying;
  }

  public Cell next() {
    return new Cell(row + rowDelta, column + columnDelta);
  }

  public boolean arrived() {
    return stopRow != null
        && stopColumn != null
        && Board.inBounds(row, column)
        && row == stopRow
        && column == stopColumn;
  }

  public void halt() {
    moving = false;
    rowDelta = 0;
    columnDelta = 0;
  }

  public void relocate(int nextRow, int nextColumn) {
    row = nextRow;
    column = nextColumn;
  }

  public void destroyAt(int nextRow, int nextColumn) {
    row = nextRow;
    column = nextColumn;
    alive = false;
    dying = true;
    moving = false;
  }

  public StoneView view() {
    return new StoneView(id, row, column, color.name(), moving && alive, dying);
  }
}
