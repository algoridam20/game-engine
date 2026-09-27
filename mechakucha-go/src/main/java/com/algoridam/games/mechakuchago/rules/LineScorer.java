package com.algoridam.games.mechakuchago.rules;

import static com.algoridam.games.mechakuchago.rules.Constants.BOARD_SIZE;
import static com.algoridam.games.mechakuchago.rules.Constants.LINE_LENGTH;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LineScorer {
  private static final int[] ACROSS = {0, 1};
  private static final int[] DOWN = {1, 0};
  private static final int[] DOWN_RIGHT = {1, 1};
  private static final int[] DOWN_LEFT = {1, -1};
  private static final int[][] RUNS = {ACROSS, DOWN, DOWN_RIGHT, DOWN_LEFT};

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
      if (hasOpenLine(board, direction)) {
        axes.add(direction);
      }
    }
    return axes;
  }

  public static List<Axis> openAxes(Board board, Color color) {
    List<Axis> open = new ArrayList<>();
    for (Direction direction : Direction.values()) {
      if (direction.owner() != color) {
        continue;
      }
      for (int index = 0; index < BOARD_SIZE; index++) {
        if (!board.axisFull(direction, index)) {
          open.add(new Axis(direction, index));
        }
      }
    }
    return open;
  }

  public static Score evaluate(Board board) {
    int blackLines = linesThatCount(board, Color.BLACK).size();
    int whiteLines = linesThatCount(board, Color.WHITE).size();
    return score(blackLines, whiteLines);
  }

  private static boolean hasOpenLine(Board board, Direction direction) {
    for (int index = 0; index < BOARD_SIZE; index++) {
      if (!board.axisFull(direction, index)) {
        return true;
      }
    }
    return false;
  }

  private static Score score(int blackLines, int whiteLines) {
    if (blackLines == 0 && whiteLines == 0) {
      return new Score(0, 0, null, false);
    }
    if (blackLines > whiteLines) {
      return new Score(blackLines, whiteLines, Color.BLACK, false);
    }
    if (whiteLines > blackLines) {
      return new Score(blackLines, whiteLines, Color.WHITE, false);
    }
    return new Score(blackLines, whiteLines, null, true);
  }

  private static List<List<Cell>> linesThatCount(Board board, Color color) {
    return linesThatDoNotShareARun(straightRuns(board, color));
  }

  private static List<List<Cell>> straightRuns(Board board, Color color) {
    List<List<Cell>> runs = new ArrayList<>();
    for (int row = 0; row < BOARD_SIZE; row++) {
      for (int column = 0; column < BOARD_SIZE; column++) {
        if (board.at(row, column) != color) {
          continue;
        }
        for (int[] step : RUNS) {
          if (continuesARunAlreadyStarted(board, color, row, column, step)) {
            continue;
          }
          List<Cell> run = runFrom(board, color, row, column, step);
          if (run.size() >= LINE_LENGTH) {
            runs.add(run);
          }
        }
      }
    }
    return runs;
  }

  private static boolean continuesARunAlreadyStarted(
      Board board, Color color, int row, int column, int[] step) {
    int previousRow = row - step[0];
    int previousColumn = column - step[1];
    return Board.inBounds(previousRow, previousColumn)
        && board.at(previousRow, previousColumn) == color;
  }

  private static List<Cell> runFrom(Board board, Color color, int row, int column, int[] step) {
    List<Cell> run = new ArrayList<>();
    int nextRow = row;
    int nextColumn = column;
    while (Board.inBounds(nextRow, nextColumn) && board.at(nextRow, nextColumn) == color) {
      run.add(new Cell(nextRow, nextColumn));
      nextRow += step[0];
      nextColumn += step[1];
    }
    return run;
  }

  private static List<List<Cell>> linesThatDoNotShareARun(List<List<Cell>> runs) {
    List<List<Cell>> counted = new ArrayList<>();
    for (List<Cell> run : runs) {
      if (!sharesMoreThanOneStone(run, counted)) {
        counted.add(run);
      }
    }
    return counted;
  }

  private static boolean sharesMoreThanOneStone(List<Cell> run, List<List<Cell>> counted) {
    for (List<Cell> earlier : counted) {
      if (sharedStoneCount(run, earlier) > 1) {
        return true;
      }
    }
    return false;
  }

  private static int sharedStoneCount(List<Cell> left, List<Cell> right) {
    Set<String> stones = new HashSet<>();
    for (Cell cell : left) {
      stones.add(cell.key());
    }
    int shared = 0;
    for (Cell cell : right) {
      if (stones.contains(cell.key())) {
        shared++;
      }
    }
    return shared;
  }

  public record Axis(Direction direction, int index) {}
}
