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
import lombok.Getter;
import lombok.experimental.Accessors;

public final class MechakuchaGoMatch {

  public enum Phase {
    CHOOSE,
    SETTLED,
    GAME_OVER
  }

  @Getter
  @Accessors(fluent = true)
  private final String blackId;

  @Getter
  @Accessors(fluent = true)
  private final String whiteId;

  private final String blackHandle;
  private final String whiteHandle;
  private final SlideResolver resolver = new SlideResolver();
  private Board board = Board.empty();
  private final Map<String, String> stoneIds = new HashMap<>();
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
      rejectPassWhenALineIsOpen(color);
    } else {
      rejectIllegalMove(color, move);
    }
    rememberLock(color, move);
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

  public synchronized PlayerView viewFor(String playerId) {
    Color color = colorOf(playerId);
    Color opponent = color.opponent();
    return new PlayerView(
        playerId,
        handleFor(color),
        color.name(),
        handleFor(opponent),
        phase.name(),
        round,
        isLocked(color),
        isLocked(opponent),
        openAxesFor(color),
        settledStones(),
        framesForPlayer(),
        score.lines(color),
        score.lines(opponent),
        winnerName(),
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
    SlideResolver.Resolution resolution = resolver.resolve(board, blackMove, whiteMove, stoneIds);
    board = resolution.board();
    frames = resolution.frames();
    rememberSettledStoneIds();
    score = LineScorer.evaluate(board);
    phase = score.gameOver() ? Phase.GAME_OVER : Phase.SETTLED;
  }

  private void rememberLock(Color color, Move move) {
    if (color == Color.BLACK) {
      blackMove = move;
      blackLocked = true;
      return;
    }
    whiteMove = move;
    whiteLocked = true;
  }

  private void rememberSettledStoneIds() {
    stoneIds.clear();
    for (StoneView stone : settledStones()) {
      stoneIds.put(stoneKey(stone.row(), stone.column()), stone.id());
    }
  }

  private void rejectPassWhenALineIsOpen(Color color) {
    if (LineScorer.hasLegalMove(board, color)) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "A legal line is still open");
    }
  }

  private void rejectIllegalMove(Color color, Move move) {
    rejectWrongEdge(color, move);
    rejectAimOffTheBoard(move);
    rejectFullLine(move);
  }

  private void rejectWrongEdge(Color color, Move move) {
    if (move.player() != color || move.direction().owner() != color) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "That edge belongs to the other player");
    }
  }

  private void rejectAimOffTheBoard(Move move) {
    if (move.index() < 0
        || move.index() >= Board.SIZE
        || move.stop() < 0
        || move.stop() >= Board.SIZE) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "The aim is off the board");
    }
  }

  private void rejectFullLine(Move move) {
    if (board.axisFull(move.direction(), move.index())) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "That line is full");
    }
  }

  private List<AxisView> openAxesFor(Color color) {
    if (phase != Phase.CHOOSE || isLocked(color)) {
      return List.of();
    }
    List<AxisView> axes = new ArrayList<>();
    for (LineScorer.Axis axis : LineScorer.openAxes(board, color)) {
      axes.add(new AxisView(axis.direction().name(), axis.index()));
    }
    return List.copyOf(axes);
  }

  private List<FrameView> framesForPlayer() {
    if (phase == Phase.CHOOSE) {
      return List.of();
    }
    return frames;
  }

  private String winnerName() {
    if (score.winner() == null) {
      return null;
    }
    return score.winner().name();
  }

  private List<StoneView> settledStones() {
    if (!frames.isEmpty()) {
      return stonesFromLastFrame();
    }
    return stonesFromBoard();
  }

  private List<StoneView> stonesFromLastFrame() {
    FrameView last = frames.get(frames.size() - 1);
    List<StoneView> live = new ArrayList<>();
    for (StoneView stone : last.stones()) {
      if (stone.dying() || !Board.inBounds(stone.row(), stone.column())) {
        continue;
      }
      live.add(new StoneView(stone.id(), stone.row(), stone.column(), stone.color(), false, false));
    }
    return live;
  }

  private List<StoneView> stonesFromBoard() {
    List<StoneView> stones = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        if (board.at(row, column) == null) {
          continue;
        }
        String id = stoneIds.getOrDefault(stoneKey(row, column), "s" + row + column);
        stones.add(new StoneView(id, row, column, board.at(row, column).name(), false, false));
      }
    }
    return stones;
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
    if (color == Color.BLACK) {
      return blackLocked;
    }
    return whiteLocked;
  }

  private String handleFor(Color color) {
    if (color == Color.BLACK) {
      return blackHandle;
    }
    return whiteHandle;
  }

  private static String stoneKey(int row, int column) {
    return row + "," + column;
  }
}
