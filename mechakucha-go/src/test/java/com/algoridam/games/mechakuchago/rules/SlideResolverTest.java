package com.algoridam.games.mechakuchago.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlideResolverTest {
  private final SlideResolver resolver = new SlideResolver();

  @Test
  void freeSlideStopsOnTheDeclaredSquare() {
    SlideResolver.Resolution board = play(null, move(Color.BLACK, Direction.RIGHT, 0, 3), null);
    assertEquals(Color.BLACK, board.board().at(0, 3));
    assertEquals(1, occupied(board.board()));
  }

  @Test
  void stoppingShortDoesNotPush() {
    Board start = Board.empty();
    start.place(0, 5, Color.WHITE);
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 0, 4), null).board();
    assertEquals(Color.BLACK, board.at(0, 4));
    assertEquals(Color.WHITE, board.at(0, 5));
  }

  @Test
  void hitShovesOneStoneAndStops() {
    Board start = Board.empty();
    start.place(0, 5, Color.WHITE);
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 0, 9), null).board();
    assertEquals(Color.BLACK, board.at(0, 5));
    assertEquals(Color.WHITE, board.at(0, 6));
  }

  @Test
  void shovesTheWholeContiguousLine() {
    Board start = Board.empty();
    start.place(2, 4, Color.WHITE);
    start.place(2, 5, Color.BLACK);
    start.place(2, 6, Color.WHITE);
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 2, 9), null).board();
    assertEquals(Color.BLACK, board.at(2, 4));
    assertEquals(Color.WHITE, board.at(2, 5));
    assertEquals(Color.BLACK, board.at(2, 6));
    assertEquals(Color.WHITE, board.at(2, 7));
  }

  @Test
  void aGapIsNotPartOfTheShovedLine() {
    Board start = Board.empty();
    start.place(2, 4, Color.WHITE);
    start.place(2, 6, Color.BLACK);
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 2, 9), null).board();
    assertEquals(Color.BLACK, board.at(2, 4));
    assertEquals(Color.WHITE, board.at(2, 5));
    assertEquals(Color.BLACK, board.at(2, 6));
  }

  @Test
  void edgeBlocksTheShove() {
    Board start = Board.empty();
    start.place(0, 9, Color.WHITE);
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 0, 9), null).board();
    assertEquals(Color.BLACK, board.at(0, 8));
    assertEquals(Color.WHITE, board.at(0, 9));
  }

  @Test
  void blackSlidesUpAndWhiteSlidesDown() {
    assertEquals(
        Color.BLACK, play(null, move(Color.BLACK, Direction.UP, 3, 6), null).board().at(6, 3));
    assertEquals(
        Color.WHITE, play(null, null, move(Color.WHITE, Direction.DOWN, 1, 4)).board().at(4, 1));
  }

  @Test
  void headOnTilesStopInAdjacentSquares() {
    Board board =
        play(
                null,
                move(Color.BLACK, Direction.RIGHT, 0, 9),
                move(Color.WHITE, Direction.LEFT, 0, 0))
            .board();
    assertEquals(Color.BLACK, board.at(0, 4));
    assertEquals(Color.WHITE, board.at(0, 5));
    assertEquals(2, occupied(board));
  }

  @Test
  void sameFrameCrossingDestroysBothTiles() {
    Board board =
        play(
                null,
                move(Color.BLACK, Direction.RIGHT, 4, 9),
                move(Color.WHITE, Direction.DOWN, 4, 9))
            .board();
    assertEquals(0, occupied(board));
  }

  @Test
  void latecomerShovesTheStoppedTile() {
    Board board =
        play(
                null,
                move(Color.BLACK, Direction.RIGHT, 4, 2),
                move(Color.WHITE, Direction.DOWN, 2, 9))
            .board();
    assertEquals(Color.WHITE, board.at(4, 2));
    assertEquals(Color.BLACK, board.at(5, 2));
  }

  @Test
  void aTileThatAlreadyPassedIsNotShoved() {
    Board board =
        play(
                null,
                move(Color.BLACK, Direction.RIGHT, 4, 9),
                move(Color.WHITE, Direction.DOWN, 2, 9))
            .board();
    assertEquals(Color.BLACK, board.at(4, 9));
    assertEquals(Color.WHITE, board.at(9, 2));
  }

  @Test
  void fullAxisMovePlacesNothingNew() {
    Board start = Board.empty();
    for (int column = 0; column < Board.SIZE; column++) {
      start.place(0, column, Color.BLACK);
    }
    assertTrue(start.axisFull(Direction.RIGHT, 0));
    Board board = play(start, move(Color.BLACK, Direction.RIGHT, 0, 3), null).board();
    for (int column = 0; column < Board.SIZE; column++) {
      assertEquals(Color.BLACK, board.at(0, column));
    }
  }

  @Test
  void fiveOrMoreCountsAsOneLineAndFourDoesNot() {
    Board five = Board.empty();
    for (int column = 0; column < 5; column++) {
      five.place(0, column, Color.BLACK);
    }
    Score score = LineScorer.evaluate(five);
    assertEquals(1, score.blackLines());
    assertEquals(Color.BLACK, score.winner());

    Board six = Board.empty();
    for (int column = 0; column < 6; column++) {
      six.place(0, column, Color.BLACK);
    }
    assertEquals(1, LineScorer.evaluate(six).blackLines());

    Board four = Board.empty();
    for (int column = 0; column < 4; column++) {
      four.place(0, column, Color.WHITE);
    }
    Score shortRun = LineScorer.evaluate(four);
    assertEquals(0, shortRun.whiteLines());
    assertNull(shortRun.winner());
    assertFalse(shortRun.draw());
  }

  @Test
  void crossingLinesCountSeparatelyAndEqualCountsDraw() {
    Board cross = Board.empty();
    for (int column = 0; column < 5; column++) {
      cross.place(4, column, Color.BLACK);
    }
    for (int row = 0; row < 5; row++) {
      cross.place(row, 2, Color.BLACK);
    }
    assertEquals(2, LineScorer.evaluate(cross).blackLines());

    Board tied = Board.empty();
    for (int column = 0; column < 5; column++) {
      tied.place(0, column, Color.BLACK);
      tied.place(1, column, Color.WHITE);
    }
    Score score = LineScorer.evaluate(tied);
    assertTrue(score.draw());
    assertNull(score.winner());
  }

  @Test
  void sameColorStonesThatMeetAreBothDestroyed() {
    Board start = Board.empty();
    start.place(4, 4, Color.WHITE);
    Board board =
        play(
                start,
                move(Color.BLACK, Direction.RIGHT, 4, 9),
                move(Color.WHITE, Direction.DOWN, 5, 4))
            .board();
    assertNull(board.at(4, 5));
    assertEquals(Color.BLACK, board.at(4, 4));
  }

  private SlideResolver.Resolution play(Board board, Move black, Move white) {
    return resolver.resolve(board == null ? Board.empty() : board, black, white);
  }

  private static Move move(Color player, Direction direction, int index, int stop) {
    return new Move(player, direction, index, stop);
  }

  private static int occupied(Board board) {
    int count = 0;
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        if (board.at(row, column) != null) {
          count++;
        }
      }
    }
    return count;
  }
}
