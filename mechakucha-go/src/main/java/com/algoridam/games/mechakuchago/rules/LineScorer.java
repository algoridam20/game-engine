package com.algoridam.games.mechakuchago.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LineScorer {
  private static final int[][] DELTAS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

  private LineScorer() {}

  public static boolean hasLegalMove(Board board, Color color) {
    return !legalAxes(board, color).isEmpty();
  }

  public static List<Direction> legalAxes(Board board, Color color) {
    List<Direction> axes = new ArrayList<>();
    for (Direction direction : Direction.values()) {
      if (direction.owner() != color) {
        continue;
      }
      for (int index = 0; index < Board.SIZE; index++) {
        if (!board.axisFull(direction, index)) {
          axes.add(direction);
          break;
        }
      }
    }
    return axes;
  }

  public static List<Axis> openAxes(Board board, Color color) {
    List<Axis> found = new ArrayList<>();
    for (Direction direction : Direction.values()) {
      if (direction.owner() != color) {
        continue;
      }
      for (int index = 0; index < Board.SIZE; index++) {
        if (!board.axisFull(direction, index)) {
          found.add(new Axis(direction, index));
        }
      }
    }
    return found;
  }

  public static Score evaluate(Board board) {
    List<List<Cell>> black = distinct(rawLines(board, Color.BLACK));
    List<List<Cell>> white = distinct(rawLines(board, Color.WHITE));
    Color winner = null;
    boolean draw = false;
    if (!black.isEmpty() || !white.isEmpty()) {
      if (black.size() > white.size()) {
        winner = Color.BLACK;
      } else if (white.size() > black.size()) {
        winner = Color.WHITE;
      } else {
        draw = true;
      }
    }
    return new Score(black.size(), white.size(), winner, draw);
  }

  private static List<List<Cell>> rawLines(Board board, Color color) {
    List<List<Cell>> lines = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        if (board.at(row, column) != color) {
          continue;
        }
        for (int[] delta : DELTAS) {
          int previousRow = row - delta[0];
          int previousColumn = column - delta[1];
          if (Board.inBounds(previousRow, previousColumn)
              && board.at(previousRow, previousColumn) == color) {
            continue;
          }
          List<Cell> cells = new ArrayList<>();
          int nextRow = row;
          int nextColumn = column;
          while (Board.inBounds(nextRow, nextColumn) && board.at(nextRow, nextColumn) == color) {
            cells.add(new Cell(nextRow, nextColumn));
            nextRow += delta[0];
            nextColumn += delta[1];
          }
          if (cells.size() >= 5) {
            lines.add(cells);
          }
        }
      }
    }
    return lines;
  }

  private static List<List<Cell>> distinct(List<List<Cell>> lines) {
    List<List<Cell>> accepted = new ArrayList<>();
    for (List<Cell> line : lines) {
      boolean ok = true;
      for (List<Cell> other : accepted) {
        if (shared(line, other) > 1) {
          ok = false;
          break;
        }
      }
      if (ok) {
        accepted.add(line);
      }
    }
    return accepted;
  }

  private static int shared(List<Cell> left, List<Cell> right) {
    Set<String> keys = new HashSet<>();
    for (Cell cell : left) {
      keys.add(cell.key());
    }
    int count = 0;
    for (Cell cell : right) {
      if (keys.contains(cell.key())) {
        count++;
      }
    }
    return count;
  }

  public record Axis(Direction direction, int index) {}
}
