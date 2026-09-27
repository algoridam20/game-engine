package com.algoridam.games.mechakuchago.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineScorerTest {

  @Test
  void emptyBoardHasNoLinesAndEveryEdgeIsOpen() {
    Board board = Board.empty();
    Score score = LineScorer.evaluate(board);

    assertEquals(0, score.blackLines());
    assertEquals(0, score.whiteLines());
    assertNull(score.winner());
    assertFalse(score.draw());
    assertFalse(score.gameOver());
    assertTrue(LineScorer.hasLegalMove(board, Color.BLACK));
    assertTrue(LineScorer.hasLegalMove(board, Color.WHITE));
    assertEquals(List.of(Direction.RIGHT, Direction.UP), LineScorer.legalAxes(board, Color.BLACK));
    assertEquals(List.of(Direction.LEFT, Direction.DOWN), LineScorer.legalAxes(board, Color.WHITE));
    assertEquals(Board.SIZE * 2, LineScorer.openAxes(board, Color.BLACK).size());
    assertEquals(Board.SIZE * 2, LineScorer.openAxes(board, Color.WHITE).size());
  }

  @Test
  void fourInARowDoesNotCountAndFiveDoes() {
    Board four = Board.empty();
    fill(four, Color.BLACK, 0, 0, 0, 1, 4);
    Score shortRun = LineScorer.evaluate(four);
    assertEquals(0, shortRun.blackLines());
    assertNull(shortRun.winner());
    assertFalse(shortRun.gameOver());

    Board five = Board.empty();
    fill(five, Color.BLACK, 0, 0, 0, 1, 5);
    Score score = LineScorer.evaluate(five);
    assertEquals(1, score.blackLines());
    assertEquals(0, score.whiteLines());
    assertEquals(Color.BLACK, score.winner());
    assertFalse(score.draw());
    assertTrue(score.gameOver());
    assertEquals(1, score.lines(Color.BLACK));
    assertEquals(0, score.lines(Color.WHITE));
  }

  @Test
  void aLongerRunIsStillOneLine() {
    Board six = Board.empty();
    fill(six, Color.BLACK, 3, 2, 0, 1, 6);
    assertEquals(1, LineScorer.evaluate(six).blackLines());

    Board fullRow = Board.empty();
    fill(fullRow, Color.WHITE, 9, 0, 0, 1, Board.SIZE);
    assertEquals(1, LineScorer.evaluate(fullRow).whiteLines());
    assertEquals(Color.WHITE, LineScorer.evaluate(fullRow).winner());
  }

  @Test
  void aGapOrAnOpponentStoneSplitsTheRun() {
    Board gap = Board.empty();
    fill(gap, Color.BLACK, 1, 0, 0, 1, 4);
    fill(gap, Color.BLACK, 1, 5, 0, 1, 4);
    assertEquals(0, LineScorer.evaluate(gap).blackLines());

    Board blocked = Board.empty();
    fill(blocked, Color.BLACK, 2, 0, 0, 1, 4);
    blocked.place(2, 4, Color.WHITE);
    fill(blocked, Color.BLACK, 2, 5, 0, 1, 4);
    Score score = LineScorer.evaluate(blocked);
    assertEquals(0, score.blackLines());
    assertEquals(0, score.whiteLines());
    assertFalse(score.gameOver());
  }

  @Test
  void verticalAndBothDiagonalsCount() {
    Board vertical = Board.empty();
    fill(vertical, Color.WHITE, 0, 7, 1, 0, 5);
    assertEquals(1, LineScorer.evaluate(vertical).whiteLines());
    assertEquals(Color.WHITE, LineScorer.evaluate(vertical).winner());

    Board downRight = Board.empty();
    fill(downRight, Color.BLACK, 0, 0, 1, 1, 5);
    assertEquals(1, LineScorer.evaluate(downRight).blackLines());

    Board downLeft = Board.empty();
    fill(downLeft, Color.BLACK, 0, 4, 1, -1, 5);
    assertEquals(1, LineScorer.evaluate(downLeft).blackLines());

    Board shortDiagonal = Board.empty();
    fill(shortDiagonal, Color.WHITE, 5, 5, 1, 1, 4);
    assertEquals(0, LineScorer.evaluate(shortDiagonal).whiteLines());
  }

  @Test
  void separateRunsCountApartAndASharedCellStillCountsBoth() {
    Board separate = Board.empty();
    fill(separate, Color.BLACK, 0, 0, 0, 1, 5);
    fill(separate, Color.BLACK, 2, 0, 0, 1, 5);
    assertEquals(2, LineScorer.evaluate(separate).blackLines());

    Board cross = Board.empty();
    fill(cross, Color.BLACK, 4, 0, 0, 1, 5);
    fill(cross, Color.BLACK, 0, 2, 1, 0, 5);
    assertEquals(2, LineScorer.evaluate(cross).blackLines());
    assertEquals(Color.BLACK, cross.at(4, 2));
  }

  @Test
  void moreLinesWinAndEqualLinesDraw() {
    Board blackAhead = Board.empty();
    fill(blackAhead, Color.BLACK, 0, 0, 0, 1, 5);
    fill(blackAhead, Color.BLACK, 1, 0, 0, 1, 5);
    fill(blackAhead, Color.WHITE, 8, 0, 0, 1, 5);
    Score ahead = LineScorer.evaluate(blackAhead);
    assertEquals(2, ahead.blackLines());
    assertEquals(1, ahead.whiteLines());
    assertEquals(Color.BLACK, ahead.winner());
    assertFalse(ahead.draw());
    assertTrue(ahead.gameOver());

    Board tied = Board.empty();
    fill(tied, Color.BLACK, 0, 0, 0, 1, 5);
    fill(tied, Color.WHITE, 1, 0, 0, 1, 5);
    Score draw = LineScorer.evaluate(tied);
    assertEquals(1, draw.blackLines());
    assertEquals(1, draw.whiteLines());
    assertNull(draw.winner());
    assertTrue(draw.draw());
    assertTrue(draw.gameOver());
  }

  @Test
  void aFullRowDropsOnlyThatLineFromTheOpenEdges() {
    Board board = Board.empty();
    fill(board, Color.WHITE, 0, 0, 0, 1, Board.SIZE);

    List<LineScorer.Axis> blackAxes = LineScorer.openAxes(board, Color.BLACK);
    assertEquals(Board.SIZE * 2 - 1, blackAxes.size());
    assertTrue(
        blackAxes.stream()
            .noneMatch(axis -> axis.direction() == Direction.RIGHT && axis.index() == 0));
    assertTrue(
        blackAxes.stream()
            .anyMatch(axis -> axis.direction() == Direction.RIGHT && axis.index() == 1));
    assertTrue(
        blackAxes.stream().anyMatch(axis -> axis.direction() == Direction.UP && axis.index() == 0));
    assertEquals(List.of(Direction.RIGHT, Direction.UP), LineScorer.legalAxes(board, Color.BLACK));
    assertTrue(LineScorer.hasLegalMove(board, Color.BLACK));
    assertTrue(LineScorer.hasLegalMove(board, Color.WHITE));
  }

  @Test
  void aFullBoardHasNoLegalMoveAndCountsEveryRun() {
    Board board = Board.empty();
    for (int row = 0; row < Board.SIZE; row++) {
      fill(board, Color.BLACK, row, 0, 0, 1, Board.SIZE);
    }

    assertFalse(LineScorer.hasLegalMove(board, Color.BLACK));
    assertFalse(LineScorer.hasLegalMove(board, Color.WHITE));
    assertTrue(LineScorer.openAxes(board, Color.BLACK).isEmpty());
    assertTrue(LineScorer.openAxes(board, Color.WHITE).isEmpty());
    assertTrue(LineScorer.legalAxes(board, Color.BLACK).isEmpty());

    Score score = LineScorer.evaluate(board);
    assertEquals(42, score.blackLines());
    assertEquals(0, score.whiteLines());
    assertEquals(Color.BLACK, score.winner());
    assertFalse(score.draw());
  }

  private static void fill(
      Board board, Color color, int row, int column, int rowDelta, int columnDelta, int count) {
    for (int step = 0; step < count; step++) {
      board.place(row + step * rowDelta, column + step * columnDelta, color);
    }
  }
}
