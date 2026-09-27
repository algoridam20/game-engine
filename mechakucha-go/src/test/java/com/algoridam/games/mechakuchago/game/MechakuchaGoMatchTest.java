package com.algoridam.games.mechakuchago.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.mechakuchago.game.dto.PlayerView;
import com.algoridam.games.mechakuchago.rules.Board;
import com.algoridam.games.mechakuchago.rules.Color;
import com.algoridam.games.mechakuchago.rules.Direction;
import com.algoridam.games.mechakuchago.rules.Move;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class MechakuchaGoMatchTest {

  @Test
  void opponentLockStaysHiddenUntilBothMovesResolve() {
    MechakuchaGoMatch match = match();
    match.lockMove("black", new Move(Color.BLACK, Direction.RIGHT, 0, 2));

    PlayerView white = match.viewFor("white");
    assertEquals("CHOOSE", white.phase());
    assertTrue(white.opponentLocked());
    assertFalse(white.youLocked());
    assertTrue(white.frames().isEmpty());
    assertTrue(white.stones().isEmpty());
    assertEquals("grace", white.handle());
    assertEquals("ada", white.opponentHandle());
    assertEquals("WHITE", white.color());
  }

  @Test
  void bothLocksPublishTheWholePathBeforeTheBoardIsSettled() {
    MechakuchaGoMatch match = match();
    match.lockMove("black", new Move(Color.BLACK, Direction.RIGHT, 4, 2));
    match.lockMove("white", new Move(Color.WHITE, Direction.DOWN, 8, 9));

    PlayerView black = match.viewFor("black");
    assertEquals("SETTLED", black.phase());
    assertTrue(black.frames().size() > 2);
    assertEquals(2, black.stones().size());
    assertEquals(
        2,
        black.frames().get(black.frames().size() - 1).stones().stream()
            .filter(stone -> !stone.dying())
            .count());
    assertEquals(0, black.yourLines());
    assertEquals(0, black.opponentLines());
    assertNull(black.winner());
    assertFalse(black.draw());
    assertFalse(black.gameOver());
  }

  @Test
  void nextRoundOpensTheNextChooseAndIgnoresASecondCall() {
    MechakuchaGoMatch match = settledOpening();

    match.nextRound();
    PlayerView black = match.viewFor("black");
    assertEquals("CHOOSE", black.phase());
    assertEquals(2, black.round());
    assertTrue(black.frames().isEmpty());
    assertFalse(black.youLocked());
    assertFalse(black.opponentLocked());
    assertEquals(2, black.stones().size());
    assertFalse(black.legalAxes().isEmpty());

    match.nextRound();
    assertEquals(2, match.viewFor("black").round());
    assertEquals("CHOOSE", match.viewFor("white").phase());
  }

  @Test
  void lockMove_RoundIsNotWaiting() {
    MechakuchaGoMatch match = settledOpening();

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("black", move(Color.BLACK, Direction.RIGHT, 1, 0)));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    assertEquals("Invalid request", exception.getMessage());
  }

  @Test
  void lockMove_MoveIsAlreadyLocked() {
    MechakuchaGoMatch match = match();
    match.lockMove("black", move(Color.BLACK, Direction.RIGHT, 0, 2));

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("black", move(Color.BLACK, Direction.RIGHT, 1, 2)));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    assertEquals("CHOOSE", match.viewFor("white").phase());
    assertFalse(match.viewFor("white").youLocked());
  }

  @Test
  void lockMove_PassWhileALineIsOpen() {
    MechakuchaGoMatch match = match();

    ServiceException exception =
        assertThrows(ServiceException.class, () -> match.lockMove("black", null));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    assertEquals("CHOOSE", match.viewFor("black").phase());
    assertFalse(match.viewFor("black").youLocked());
  }

  @Test
  void lockMove_PassIsAcceptedWhenNoLineIsOpen() throws Exception {
    MechakuchaGoMatch match = match();
    setBoard(match, fullBoard(Color.BLACK));

    match.lockMove("black", null);
    PlayerView black = match.viewFor("black");
    assertEquals("CHOOSE", black.phase());
    assertTrue(black.youLocked());
    assertTrue(black.legalAxes().isEmpty());
    assertFalse(match.viewFor("white").youLocked());

    match.lockMove("white", null);
    PlayerView settled = match.viewFor("black");
    assertEquals("GAME_OVER", settled.phase());
    assertEquals(42, settled.yourLines());
    assertEquals(0, settled.opponentLines());
    assertEquals("BLACK", settled.winner());
    assertTrue(settled.gameOver());
  }

  @Test
  void lockMove_EdgeBelongsToTheOtherPlayer() {
    MechakuchaGoMatch match = match();

    ServiceException wrongDirection =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("black", move(Color.BLACK, Direction.LEFT, 0, 2)));
    ServiceException wrongColor =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("black", move(Color.WHITE, Direction.DOWN, 0, 2)));
    ServiceException whiteOnBlackEdge =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("white", move(Color.WHITE, Direction.UP, 0, 2)));

    assertEquals(ErrorCode.BAD_REQUEST, wrongDirection.getErrorCode());
    assertEquals(ErrorCode.BAD_REQUEST, wrongColor.getErrorCode());
    assertEquals(ErrorCode.BAD_REQUEST, whiteOnBlackEdge.getErrorCode());
    assertFalse(match.viewFor("black").youLocked());
    assertFalse(match.viewFor("white").youLocked());
  }

  @Test
  void lockMove_AimIsOffTheBoard() {
    MechakuchaGoMatch match = match();
    assertRejected(match, move(Color.BLACK, Direction.RIGHT, -1, 0));
    assertRejected(match, move(Color.BLACK, Direction.RIGHT, Board.SIZE, 0));
    assertRejected(match, move(Color.BLACK, Direction.RIGHT, 0, -1));
    assertRejected(match, move(Color.BLACK, Direction.RIGHT, 0, Board.SIZE));
    assertRejected(match, move(Color.WHITE, Direction.DOWN, 0, -1));
    assertEquals("CHOOSE", match.viewFor("black").phase());
  }

  @Test
  void lockMove_LineIsFull() throws Exception {
    MechakuchaGoMatch match = match();
    Board board = Board.empty();
    for (int column = 0; column < Board.SIZE; column++) {
      board.place(0, column, Color.WHITE);
    }
    setBoard(match, board);

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("black", move(Color.BLACK, Direction.RIGHT, 0, 3)));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());

    match.lockMove("black", move(Color.BLACK, Direction.RIGHT, 1, 0));
    assertTrue(match.viewFor("black").youLocked());
    assertEquals("CHOOSE", match.viewFor("black").phase());
  }

  @Test
  void lockMove_PlayerIsNotInThisMatch() {
    MechakuchaGoMatch match = match();

    ServiceException lock =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("spectator", move(Color.BLACK, Direction.RIGHT, 0, 0)));
    ServiceException view = assertThrows(ServiceException.class, () -> match.viewFor("spectator"));

    assertEquals(ErrorCode.FORBIDDEN, lock.getErrorCode());
    assertEquals(ErrorCode.FORBIDDEN, view.getErrorCode());
    assertEquals("Operation is not allowed", lock.getMessage());
  }

  @Test
  void chooseViewListsOpenLinesUntilThatPlayerLocks() {
    MechakuchaGoMatch match = match();
    PlayerView black = match.viewFor("black");
    assertEquals("ada", black.handle());
    assertEquals("grace", black.opponentHandle());
    assertEquals("BLACK", black.color());
    assertEquals(1, black.round());
    assertEquals(Board.SIZE * 2, black.legalAxes().size());
    assertEquals("RIGHT", black.legalAxes().get(0).direction());
    assertEquals(0, black.legalAxes().get(0).index());
    assertTrue(black.stones().isEmpty());
    assertTrue(black.frames().isEmpty());

    match.lockMove("black", move(Color.BLACK, Direction.UP, 1, 4));
    assertTrue(match.viewFor("black").legalAxes().isEmpty());
    assertEquals(Board.SIZE * 2, match.viewFor("white").legalAxes().size());
    assertEquals("LEFT", match.viewFor("white").legalAxes().get(0).direction());
  }

  @Test
  void completeSuccessfulGame_BlackMakesFive() {
    MechakuchaGoMatch match = match();

    play(match, move(Color.BLACK, Direction.RIGHT, 0, 4), move(Color.WHITE, Direction.DOWN, 9, 0));
    assertEquals("SETTLED", match.viewFor("black").phase());
    assertStone(match, 0, 4, "BLACK");
    assertStone(match, 0, 9, "WHITE");
    assertEquals(0, match.viewFor("black").yourLines());

    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 3), move(Color.WHITE, Direction.DOWN, 8, 2));
    assertStone(match, 0, 3, "BLACK");
    assertStone(match, 0, 4, "BLACK");
    assertStone(match, 2, 8, "WHITE");

    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 2), move(Color.WHITE, Direction.DOWN, 7, 4));
    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 1), move(Color.WHITE, Direction.DOWN, 6, 6));
    assertEquals("SETTLED", match.viewFor("black").phase());
    assertEquals(4, match.viewFor("black").round());
    assertEquals(0, match.viewFor("black").yourLines());
    assertFalse(match.viewFor("black").gameOver());

    match.nextRound();
    assertEquals("CHOOSE", match.viewFor("black").phase());
    assertEquals(8, match.viewFor("black").stones().size());
    assertTrue(match.viewFor("black").frames().isEmpty());

    play(match, move(Color.BLACK, Direction.RIGHT, 0, 0), move(Color.WHITE, Direction.DOWN, 5, 8));

    PlayerView black = match.viewFor("black");
    PlayerView white = match.viewFor("white");
    assertEquals("GAME_OVER", black.phase());
    assertEquals(5, black.round());
    assertEquals(1, black.yourLines());
    assertEquals(0, black.opponentLines());
    assertEquals("BLACK", black.winner());
    assertFalse(black.draw());
    assertTrue(black.gameOver());
    assertTrue(black.legalAxes().isEmpty());
    assertEquals(10, black.stones().size());
    assertFalse(black.frames().isEmpty());

    assertEquals(0, white.yourLines());
    assertEquals(1, white.opponentLines());
    assertEquals("BLACK", white.winner());
    assertTrue(white.gameOver());
    assertStone(match, 0, 0, "BLACK");
    assertStone(match, 0, 1, "BLACK");
    assertStone(match, 0, 2, "BLACK");
    assertStone(match, 0, 3, "BLACK");
    assertStone(match, 0, 4, "BLACK");
    assertStone(match, 8, 5, "WHITE");

    match.nextRound();
    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> match.lockMove("white", move(Color.WHITE, Direction.LEFT, 1, 1)));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    assertEquals(5, match.viewFor("white").round());
    assertEquals("GAME_OVER", match.viewFor("black").phase());
  }

  @Test
  void completeGame_EqualLinesDraw() {
    MechakuchaGoMatch match = match();

    play(match, move(Color.BLACK, Direction.RIGHT, 0, 4), move(Color.WHITE, Direction.LEFT, 9, 4));
    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 3), move(Color.WHITE, Direction.LEFT, 9, 5));
    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 2), move(Color.WHITE, Direction.LEFT, 9, 6));
    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 1), move(Color.WHITE, Direction.LEFT, 9, 7));
    assertEquals(0, match.viewFor("black").yourLines());
    assertEquals(0, match.viewFor("white").yourLines());
    assertEquals("SETTLED", match.viewFor("white").phase());

    match.nextRound();
    play(match, move(Color.BLACK, Direction.RIGHT, 0, 0), move(Color.WHITE, Direction.LEFT, 9, 8));

    PlayerView black = match.viewFor("black");
    PlayerView white = match.viewFor("white");
    assertEquals("GAME_OVER", black.phase());
    assertEquals(1, black.yourLines());
    assertEquals(1, black.opponentLines());
    assertNull(black.winner());
    assertTrue(black.draw());
    assertTrue(black.gameOver());
    assertEquals(1, white.yourLines());
    assertEquals(1, white.opponentLines());
    assertTrue(white.draw());
    assertStone(match, 0, 0, "BLACK");
    assertStone(match, 9, 8, "WHITE");
    assertEquals(10, black.stones().size());
  }

  @Test
  void nextRound_BothPlayersWithNoOpenLineFinishImmediately() throws Exception {
    MechakuchaGoMatch match = settledOpening();
    setBoard(match, fullBoard(Color.WHITE));

    match.nextRound();

    PlayerView white = match.viewFor("white");
    assertEquals(2, white.round());
    assertEquals("GAME_OVER", white.phase());
    assertEquals(42, white.yourLines());
    assertEquals(0, white.opponentLines());
    assertEquals("WHITE", white.winner());
    assertEquals(Board.SIZE * Board.SIZE, white.stones().size());
    assertEquals("WHITE", match.viewFor("black").winner());
  }

  private static MechakuchaGoMatch match() {
    return new MechakuchaGoMatch("black", "white", "ada", "grace");
  }

  private static MechakuchaGoMatch settledOpening() {
    MechakuchaGoMatch match = match();
    play(match, move(Color.BLACK, Direction.RIGHT, 4, 2), move(Color.WHITE, Direction.DOWN, 8, 9));
    return match;
  }

  private static void play(MechakuchaGoMatch match, Move black, Move white) {
    match.lockMove("black", black);
    match.lockMove("white", white);
  }

  private static Move move(Color player, Direction direction, int index, int stop) {
    return new Move(player, direction, index, stop);
  }

  private static void assertRejected(MechakuchaGoMatch match, Move move) {
    String playerId = move.player() == Color.BLACK ? "black" : "white";
    ServiceException exception =
        assertThrows(ServiceException.class, () -> match.lockMove(playerId, move));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
  }

  private static void assertStone(MechakuchaGoMatch match, int row, int column, String color) {
    assertTrue(
        match.viewFor("black").stones().stream()
            .anyMatch(
                stone ->
                    stone.row() == row
                        && stone.column() == column
                        && color.equals(stone.color())
                        && !stone.dying()));
  }

  private static Board fullBoard(Color color) {
    Board board = Board.empty();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        board.place(row, column, color);
      }
    }
    return board;
  }

  private static void setBoard(MechakuchaGoMatch match, Board board) throws Exception {
    Field field = MechakuchaGoMatch.class.getDeclaredField("board");
    field.setAccessible(true);
    field.set(match, board);
  }
}
