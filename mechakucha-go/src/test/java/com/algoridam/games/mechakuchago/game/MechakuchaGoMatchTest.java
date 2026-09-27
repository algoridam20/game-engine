package com.algoridam.games.mechakuchago.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.algoridam.games.mechakuchago.game.dto.PlayerView;
import com.algoridam.games.mechakuchago.rules.Color;
import com.algoridam.games.mechakuchago.rules.Direction;
import com.algoridam.games.mechakuchago.rules.Move;
import org.junit.jupiter.api.Test;

class MechakuchaGoMatchTest {

  @Test
  void opponentLockStaysHiddenUntilBothMovesResolve() {
    MechakuchaGoMatch match = new MechakuchaGoMatch("black", "white", "ada", "grace");
    match.lockMove("black", new Move(Color.BLACK, Direction.RIGHT, 0, 2));

    PlayerView white = match.viewFor("white");
    assertEquals("CHOOSE", white.phase());
    assertTrue(white.opponentLocked());
    assertFalse(white.youLocked());
    assertTrue(white.frames().isEmpty());
    assertTrue(white.stones().isEmpty());
  }

  @Test
  void bothLocksPublishTheWholePathBeforeTheBoardIsSettled() {
    MechakuchaGoMatch match = new MechakuchaGoMatch("black", "white", "ada", "grace");
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
  }

  @Test
  void nextRoundOpensTheNextChooseAndIgnoresASecondCall() {
    MechakuchaGoMatch match = new MechakuchaGoMatch("black", "white", "ada", "grace");
    match.lockMove("black", new Move(Color.BLACK, Direction.RIGHT, 4, 2));
    match.lockMove("white", new Move(Color.WHITE, Direction.DOWN, 8, 9));

    match.nextRound();
    PlayerView black = match.viewFor("black");
    assertEquals("CHOOSE", black.phase());
    assertEquals(2, black.round());
    assertTrue(black.frames().isEmpty());

    match.nextRound();
    assertEquals(2, match.viewFor("black").round());
    assertEquals("CHOOSE", match.viewFor("white").phase());
  }
}
