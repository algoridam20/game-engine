package com.algoridam.games.mechakuchago.game;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.mechakuchago.game.dto.AxisView;
import com.algoridam.games.mechakuchago.game.dto.PlayerView;
import com.algoridam.games.mechakuchago.rules.Board;
import com.algoridam.games.mechakuchago.rules.Color;
import com.algoridam.games.mechakuchago.rules.FrameView;
import com.algoridam.games.mechakuchago.rules.LineScorer;
import com.algoridam.games.mechakuchago.rules.Move;
import com.algoridam.games.mechakuchago.rules.Score;
import com.algoridam.games.mechakuchago.rules.SlideResolver;
import com.algoridam.games.mechakuchago.rules.StoneView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MechakuchaGoMatch {
  public enum Phase {
    CHOOSE,
    SETTLED,
    GAME_OVER
  }

  private final String blackId;
  private final String whiteId;
  private final String blackHandle;
  private final String whiteHandle;
  private final SlideResolver resolver = new SlideResolver();
  private Board board = Board.empty();
  private final Map<String, String> reuseIds = new HashMap<>();
  private Move blackMove;
  private Move whiteMove;
  private boolean blackLocked;
  private boolean whiteLocked;
  private Phase phase = Phase.CHOOSE;
  private int round = 1;
  private List<FrameView> frames = List.of();
  private Score score = LineScorer.evaluate(board);

  public MechakuchaGoMatch(String blackId, String whiteId, String blackHandle, String whiteHandle) {
    this.blackId = blackId;
    this.whiteId = whiteId;
    this.blackHandle = blackHandle;
    this.whiteHandle = whiteHandle;
    beginRound();
  }

  public synchronized void lockMove(String playerId, Move move) {
    if (phase != Phase.CHOOSE) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "The round is not waiting for a move");
    }
    Color color = colorOf(playerId);
    if (isLocked(color)) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Move is already locked");
    }
    if (move == null) {
      if (LineScorer.hasLegalMove(board, color)) {
        throw new ServiceException(ErrorCode.BAD_REQUEST, "A legal line is still open");
      }
    } else {
      validate(color, move);
    }
    if (color == Color.BLACK) {
      blackMove = move;
      blackLocked = true;
    } else {
      whiteMove = move;
      whiteLocked = true;
    }
    if (blackLocked && whiteLocked) {
      finishRound();
    }
  }

  public synchronized void nextRound() {
    if (phase != Phase.SETTLED) {
      return;
    }
    round += 1;
    beginRound();
  }

  public String blackId() {
    return blackId;
  }

  public String whiteId() {
    return whiteId;
  }

  public synchronized PlayerView viewFor(String playerId) {
    Color color = colorOf(playerId);
    Color opponent = color.opponent();
    List<AxisView> axes = new ArrayList<>();
    if (phase == Phase.CHOOSE && !isLocked(color)) {
      for (LineScorer.Axis axis : LineScorer.openAxes(board, color)) {
        axes.add(new AxisView(axis.direction().name(), axis.index()));
      }
    }
    String winner = score.winner() == null ? null : score.winner().name();
    return new PlayerView(
        playerId,
        handleOf(color),
        color.name(),
        handleOf(opponent),
        phase.name(),
        round,
        isLocked(color),
        isLocked(opponent),
        List.copyOf(axes),
        settledStones(),
        phase == Phase.CHOOSE ? List.of() : frames,
        score.lines(color),
        score.lines(opponent),
        winner,
        score.draw(),
        phase == Phase.GAME_OVER);
  }

  private void beginRound() {
    blackMove = null;
    whiteMove = null;
    blackLocked = !LineScorer.hasLegalMove(board, Color.BLACK);
    whiteLocked = !LineScorer.hasLegalMove(board, Color.WHITE);
    frames = List.of();
    if (blackLocked && whiteLocked) {
      finishRound();
      return;
    }
    phase = Phase.CHOOSE;
  }

  private void finishRound() {
    SlideResolver.Resolution resolution = resolver.resolve(board, blackMove, whiteMove, reuseIds);
    board = resolution.board();
    frames = resolution.frames();
    reuseIds.clear();
    for (StoneView stone : settledStones()) {
      reuseIds.put(stone.row() + "," + stone.column(), stone.id());
    }
    score = LineScorer.evaluate(board);
    phase = score.gameOver() ? Phase.GAME_OVER : Phase.SETTLED;
  }

  private List<StoneView> settledStones() {
    if (!frames.isEmpty()) {
      FrameView last = frames.get(frames.size() - 1);
      List<StoneView> live = new ArrayList<>();
      for (StoneView stone : last.stones()) {
        if (!stone.dying() && Board.inBounds(stone.row(), stone.column())) {
          live.add(
              new StoneView(stone.id(), stone.row(), stone.column(), stone.color(), false, false));
        }
      }
      return live;
    }
    List<StoneView> stones = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        if (board.at(row, column) == null) {
          continue;
        }
        String id = reuseIds.getOrDefault(row + "," + column, "s" + row + column);
        stones.add(new StoneView(id, row, column, board.at(row, column).name(), false, false));
      }
    }
    return stones;
  }

  private void validate(Color color, Move move) {
    if (move.player() != color || move.direction().owner() != color) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "That edge belongs to the other player");
    }
    if (move.index() < 0
        || move.index() >= Board.SIZE
        || move.stop() < 0
        || move.stop() >= Board.SIZE) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "The aim is off the board");
    }
    if (board.axisFull(move.direction(), move.index())) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "That line is full");
    }
  }

  private Color colorOf(String playerId) {
    if (blackId.equals(playerId)) {
      return Color.BLACK;
    }
    if (whiteId.equals(playerId)) {
      return Color.WHITE;
    }
    throw new ServiceException(ErrorCode.FORBIDDEN, "Player is not in this match");
  }

  private boolean isLocked(Color color) {
    return color == Color.BLACK ? blackLocked : whiteLocked;
  }

  private String handleOf(Color color) {
    return color == Color.BLACK ? blackHandle : whiteHandle;
  }
}
