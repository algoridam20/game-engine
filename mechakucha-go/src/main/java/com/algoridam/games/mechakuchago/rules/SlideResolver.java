package com.algoridam.games.mechakuchago.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SlideResolver {
  private int sequence = 1;

  public Resolution resolve(Board board, Move black, Move white) {
    return resolve(board, black, white, Map.of());
  }

  public Resolution resolve(Board board, Move black, Move white, Map<String, String> reuseIds) {
    List<Stone> pieces = new ArrayList<>();
    for (int row = 0; row < Board.SIZE; row++) {
      for (int column = 0; column < Board.SIZE; column++) {
        Color color = board.at(row, column);
        if (color == null) {
          continue;
        }
        String key = row + "," + column;
        String id = reuseIds.getOrDefault(key, "s" + sequence++);
        pieces.add(Stone.sitting(id, color, row, column));
      }
    }

    List<String> ignored = new ArrayList<>();
    consider(board, pieces, black, ignored);
    consider(board, pieces, white, ignored);

    List<FrameView> frames = new ArrayList<>();
    List<String> opening = new ArrayList<>(ignored);
    opening.add("Tiles appear at the edge and start sliding.");
    frames.add(snapshot(pieces, opening));

    for (int frame = 0; frame < 24; frame++) {
      List<Stone> active = new ArrayList<>();
      for (Stone piece : pieces) {
        if (piece.alive() && piece.moving()) {
          active.add(piece);
        }
      }
      if (active.isEmpty()) {
        break;
      }

      Map<String, Plan> plans = new HashMap<>();
      List<String> events = new ArrayList<>();
      for (Stone piece : active) {
        if (piece.arrived()) {
          plans.put(piece.id(), Plan.stay("arrived", new Cell(piece.row(), piece.column())));
        }
      }

      List<Stone> still = new ArrayList<>();
      for (Stone piece : active) {
        if (!plans.containsKey(piece.id())) {
          still.add(piece);
        }
      }
      if (still.size() == 2) {
        resolvePair(pieces, still.get(0), still.get(1), plans, events);
      }

      List<Stone> pending = new ArrayList<>();
      for (Stone piece : still) {
        if (!plans.containsKey(piece.id())) {
          pending.add(piece);
        }
      }
      int guard = 0;
      while (needsDecision(pending, plans) && guard < 6) {
        guard++;
        for (Stone piece : orderPending(pending)) {
          Plan existing = plans.get(piece.id());
          if (existing != null && existing.type() != Plan.Type.RETRY) {
            continue;
          }
          plans.put(piece.id(), decide(piece, pieces, plans));
        }
      }
      for (Stone piece : pending) {
        Plan plan = plans.get(piece.id());
        if (plan == null || plan.type() == Plan.Type.RETRY) {
          plans.put(piece.id(), Plan.stay("blocked", new Cell(piece.row(), piece.column())));
        }
      }
      lockSharedChains(pieces, plans, events);
      applyPlans(pieces, plans, events);
      frames.add(snapshot(pieces, events));
      pieces.removeIf(piece -> !piece.alive());
    }

    if (frames.get(frames.size() - 1).stones().stream().anyMatch(StoneView::dying)) {
      frames.add(snapshot(pieces, List.of("The board settles.")));
    }
    return new Resolution(boardFrom(pieces), List.copyOf(frames));
  }

  private void consider(Board board, List<Stone> pieces, Move move, List<String> ignored) {
    if (move == null) {
      return;
    }
    if (board.axisFull(move.direction(), move.index())) {
      ignored.add(move.player().label() + " chose a full line. The move is ignored.");
      return;
    }
    pieces.add(Stone.mover("m" + sequence++, move));
  }

  private static void resolvePair(
      List<Stone> pieces, Stone first, Stone second, Map<String, Plan> plans, List<String> events) {
    Cell firstNext = first.next();
    Cell secondNext = second.next();
    boolean swap =
        firstNext.row() == second.row()
            && firstNext.column() == second.column()
            && secondNext.row() == first.row()
            && secondNext.column() == first.column();
    boolean same =
        firstNext.row() == secondNext.row()
            && firstNext.column() == secondNext.column()
            && Board.inBounds(firstNext.row(), firstNext.column());
    if (swap) {
      plans.put(first.id(), Plan.stay("headon", new Cell(first.row(), first.column())));
      plans.put(second.id(), Plan.stay("headon", new Cell(second.row(), second.column())));
      events.add("The tiles meet face to face in adjacent squares. Both lines stop.");
    } else if (same && pieceAt(pieces, firstNext.row(), firstNext.column(), null) == null) {
      plans.put(first.id(), Plan.die(firstNext));
      plans.put(second.id(), Plan.die(secondNext));
      events.add(
          "Both tiles enter "
              + Board.cellName(firstNext.row(), firstNext.column())
              + " on the same frame and destroy each other.");
    }
  }

  private static boolean needsDecision(List<Stone> pending, Map<String, Plan> plans) {
    for (Stone piece : pending) {
      Plan plan = plans.get(piece.id());
      if (plan == null || plan.type() == Plan.Type.RETRY) {
        return true;
      }
    }
    return false;
  }

  private static List<Stone> orderPending(List<Stone> pending) {
    List<Stone> ordered = new ArrayList<>(pending);
    ordered.sort(
        (left, right) -> {
          Cell leftNext = left.next();
          boolean leftHitsRight =
              leftNext.row() == right.row() && leftNext.column() == right.column();
          Cell rightNext = right.next();
          boolean rightHitsLeft =
              rightNext.row() == left.row() && rightNext.column() == left.column();
          if (leftHitsRight && !rightHitsLeft) {
            return 1;
          }
          if (rightHitsLeft && !leftHitsRight) {
            return -1;
          }
          return 0;
        });
    return ordered;
  }

  private static Plan decide(Stone piece, List<Stone> pieces, Map<String, Plan> plans) {
    Cell next = piece.next();
    if (!Board.inBounds(next.row(), next.column())) {
      return Plan.stay("blocked", next);
    }
    Stone occupied = pieceAt(pieces, next.row(), next.column(), piece.id());
    if (occupied == null) {
      return Plan.move(next);
    }
    if (occupied.moving()) {
      Plan other = plans.get(occupied.id());
      if (other == null || other.type() == Plan.Type.RETRY) {
        return Plan.retry();
      }
      if (other.type() == Plan.Type.STAY) {
        return pushFrom(piece, pieces, next, plans);
      }
      return Plan.move(next);
    }
    return pushFrom(piece, pieces, next, plans);
  }

  private static Plan pushFrom(Stone piece, List<Stone> pieces, Cell hit, Map<String, Plan> plans) {
    Chain chain = chainAhead(pieces, hit, piece.rowDelta(), piece.columnDelta(), plans);
    if (chain.stones().isEmpty()) {
      return Plan.move(hit);
    }
    if (!Board.inBounds(chain.beyond().row(), chain.beyond().column())) {
      return Plan.stay("blocked", new Cell(piece.row(), piece.column()));
    }
    Stone blocker = pieceAt(pieces, chain.beyond().row(), chain.beyond().column(), null);
    if (blocker != null) {
      if (!blocker.moving()) {
        return Plan.stay("blocked", new Cell(piece.row(), piece.column()));
      }
      Plan other = plans.get(blocker.id());
      if (other == null || other.type() == Plan.Type.RETRY) {
        Cell otherNext = blocker.next();
        boolean aimingIntoLine =
            chain.stones().stream()
                .anyMatch(
                    stone ->
                        stone.row() == otherNext.row() && stone.column() == otherNext.column());
        if (!aimingIntoLine) {
          return Plan.retry();
        }
      } else if (other.type() == Plan.Type.STAY || other.type() == Plan.Type.DIE) {
        return Plan.stay("blocked", new Cell(piece.row(), piece.column()));
      }
    }
    return Plan.push(hit, piece.rowDelta(), piece.columnDelta(), chain.stones());
  }

  private static Chain chainAhead(
      List<Stone> pieces, Cell hit, int rowDelta, int columnDelta, Map<String, Plan> plans) {
    List<Stone> chain = new ArrayList<>();
    int row = hit.row();
    int column = hit.column();
    while (Board.inBounds(row, column)) {
      Stone occupied = pieceAt(pieces, row, column, null);
      if (occupied == null) {
        break;
      }
      if (occupied.moving()) {
        Plan plan = plans.get(occupied.id());
        if (plan == null || plan.type() != Plan.Type.STAY) {
          break;
        }
      }
      chain.add(occupied);
      row += rowDelta;
      column += columnDelta;
    }
    return new Chain(chain, new Cell(row, column));
  }

  private static void lockSharedChains(
      List<Stone> pieces, Map<String, Plan> plans, List<String> events) {
    List<Map.Entry<String, Plan>> pushers = new ArrayList<>();
    for (Map.Entry<String, Plan> entry : plans.entrySet()) {
      if (entry.getValue().type() == Plan.Type.PUSH) {
        pushers.add(entry);
      }
    }
    if (pushers.size() != 2) {
      return;
    }
    Set<String> ids = new HashSet<>();
    for (Stone stone : pushers.get(0).getValue().chain()) {
      ids.add(stone.id());
    }
    boolean shares = false;
    for (Stone stone : pushers.get(1).getValue().chain()) {
      if (ids.contains(stone.id())) {
        shares = true;
        break;
      }
    }
    if (!shares) {
      return;
    }
    Stone first = find(pieces, pushers.get(0).getKey());
    Stone second = find(pieces, pushers.get(1).getKey());
    plans.put(first.id(), Plan.stay("lock", new Cell(first.row(), first.column())));
    plans.put(second.id(), Plan.stay("lock", new Cell(second.row(), second.column())));
    events.add("Both tiles shove the same line on this frame. The line locks and both stop.");
  }

  private static void applyPlans(List<Stone> pieces, Map<String, Plan> plans, List<String> events) {
    Map<String, Plan> pushed = new HashMap<>();
    for (Plan plan : plans.values()) {
      if (plan.type() != Plan.Type.PUSH) {
        continue;
      }
      for (Stone stone : plan.chain()) {
        pushed.put(stone.id(), plan);
      }
    }

    List<Shift> moves = new ArrayList<>();
    for (Stone piece : pieces) {
      if (!piece.alive()) {
        continue;
      }
      Plan plan = plans.get(piece.id());
      if (plan != null && plan.type() == Plan.Type.DIE) {
        continue;
      }
      if (plan != null && plan.type() == Plan.Type.MOVE) {
        moves.add(new Shift(piece, plan.row(), plan.column(), "move", plan));
        continue;
      }
      if (plan != null && plan.type() == Plan.Type.PUSH) {
        moves.add(new Shift(piece, plan.row(), plan.column(), "push", plan));
        continue;
      }
      Plan shove = pushed.get(piece.id());
      if (shove != null) {
        moves.add(
            new Shift(
                piece,
                piece.row() + shove.rowDelta(),
                piece.column() + shove.columnDelta(),
                "shift",
                shove));
      }
    }

    Map<String, List<Shift>> groups = new HashMap<>();
    for (Shift move : moves) {
      groups.computeIfAbsent(move.row + "," + move.column, key -> new ArrayList<>()).add(move);
    }
    Set<String> doomed = new HashSet<>();
    for (List<Shift> group : groups.values()) {
      if (group.size() > 1) {
        Shift sample = group.get(0);
        events.add(
            "Two tiles both enter "
                + Board.cellName(sample.row, sample.column)
                + " and are destroyed.");
        for (Shift move : group) {
          doomed.add(move.stone.id());
        }
      }
    }

    Set<String> stayCells = new HashSet<>();
    for (Stone piece : pieces) {
      if (!piece.alive() || doomed.contains(piece.id())) {
        continue;
      }
      if (moves.stream().anyMatch(move -> move.stone.id().equals(piece.id()))) {
        continue;
      }
      Plan plan = plans.get(piece.id());
      if (plan != null && plan.type() == Plan.Type.DIE) {
        continue;
      }
      if (Board.inBounds(piece.row(), piece.column())) {
        stayCells.add(piece.row() + "," + piece.column());
      }
    }
    for (Shift move : moves) {
      if (doomed.contains(move.stone.id())) {
        continue;
      }
      if (stayCells.contains(move.row + "," + move.column)) {
        doomed.add(move.stone.id());
        events.add(
            move.stone.color().label()
                + " cannot enter "
                + Board.cellName(move.row, move.column)
                + " and is destroyed.");
      }
    }

    for (Shift move : moves) {
      if (doomed.contains(move.stone.id())) {
        move.stone.destroyAt(move.row, move.column);
        continue;
      }
      move.stone.relocate(move.row, move.column);
      if ("move".equals(move.cause)) {
        boolean arrived = move.stone.arrived();
        if (arrived) {
          move.stone.halt();
        }
        events.add(
            move.stone.color().label()
                + " slides to "
                + Board.cellName(move.stone.row(), move.stone.column())
                + (arrived ? " and stops." : "."));
      } else if ("push".equals(move.cause)) {
        move.stone.halt();
        events.add(
            move.stone.color().label()
                + " shoves a line of "
                + move.plan.chain().size()
                + " one square and stops at "
                + Board.cellName(move.stone.row(), move.stone.column())
                + ".");
      } else if (move.stone.moving()) {
        move.stone.halt();
      }
    }

    for (Map.Entry<String, Plan> entry : plans.entrySet()) {
      Plan plan = entry.getValue();
      if (plan.type() != Plan.Type.DIE && plan.type() != Plan.Type.STAY) {
        continue;
      }
      Stone piece = find(pieces, entry.getKey());
      if (piece == null) {
        continue;
      }
      if (plan.type() == Plan.Type.DIE) {
        piece.destroyAt(plan.at().row(), plan.at().column());
        continue;
      }
      piece.halt();
      if (!Board.inBounds(piece.row(), piece.column())) {
        if (plan.intent() != null && Board.inBounds(plan.intent().row(), plan.intent().column())) {
          piece.relocate(plan.intent().row(), plan.intent().column());
        }
        piece.destroyAt(piece.row(), piece.column());
        events.add(piece.color().label() + " cannot enter the line and is lost.");
      } else if ("blocked".equals(plan.reason())) {
        events.add(
            piece.color().label()
                + " is packed against the edge and stops at "
                + Board.cellName(piece.row(), piece.column())
                + ".");
      }
    }
  }

  private static FrameView snapshot(List<Stone> pieces, List<String> events) {
    List<StoneView> stones = new ArrayList<>();
    for (Stone piece : pieces) {
      if (!piece.alive() && !piece.dying()) {
        continue;
      }
      boolean visible =
          piece.dying() || piece.moving() || Board.inBounds(piece.row(), piece.column());
      if (visible) {
        stones.add(piece.view());
      }
    }
    return new FrameView(List.copyOf(events), List.copyOf(stones));
  }

  private static Board boardFrom(List<Stone> pieces) {
    Board board = Board.empty();
    for (Stone piece : pieces) {
      if (piece.alive() && Board.inBounds(piece.row(), piece.column())) {
        board.place(piece.row(), piece.column(), piece.color());
      }
    }
    return board;
  }

  private static Stone pieceAt(List<Stone> pieces, int row, int column, String ignoreId) {
    for (Stone piece : pieces) {
      if (!piece.alive() || piece.id().equals(ignoreId)) {
        continue;
      }
      if (piece.row() == row && piece.column() == column) {
        return piece;
      }
    }
    return null;
  }

  private static Stone find(List<Stone> pieces, String id) {
    for (Stone piece : pieces) {
      if (piece.id().equals(id)) {
        return piece;
      }
    }
    return null;
  }

  public record Resolution(Board board, List<FrameView> frames) {}

  private record Chain(List<Stone> stones, Cell beyond) {}

  private static final class Shift {
    private final Stone stone;
    private final int row;
    private final int column;
    private final String cause;
    private final Plan plan;

    private Shift(Stone stone, int row, int column, String cause, Plan plan) {
      this.stone = stone;
      this.row = row;
      this.column = column;
      this.cause = cause;
      this.plan = plan;
    }
  }

  private static final class Plan {
    private enum Type {
      MOVE,
      PUSH,
      STAY,
      DIE,
      RETRY
    }

    private final Type type;
    private final int row;
    private final int column;
    private final int rowDelta;
    private final int columnDelta;
    private final List<Stone> chain;
    private final String reason;
    private final Cell intent;
    private final Cell at;

    private Plan(
        Type type,
        int row,
        int column,
        int rowDelta,
        int columnDelta,
        List<Stone> chain,
        String reason,
        Cell intent,
        Cell at) {
      this.type = type;
      this.row = row;
      this.column = column;
      this.rowDelta = rowDelta;
      this.columnDelta = columnDelta;
      this.chain = chain;
      this.reason = reason;
      this.intent = intent;
      this.at = at;
    }

    private static Plan move(Cell cell) {
      return new Plan(Type.MOVE, cell.row(), cell.column(), 0, 0, List.of(), null, null, null);
    }

    private static Plan push(Cell cell, int rowDelta, int columnDelta, List<Stone> chain) {
      return new Plan(
          Type.PUSH, cell.row(), cell.column(), rowDelta, columnDelta, chain, null, null, null);
    }

    private static Plan stay(String reason, Cell intent) {
      return new Plan(
          Type.STAY, intent.row(), intent.column(), 0, 0, List.of(), reason, intent, null);
    }

    private static Plan die(Cell at) {
      return new Plan(Type.DIE, at.row(), at.column(), 0, 0, List.of(), null, null, at);
    }

    private static Plan retry() {
      return new Plan(Type.RETRY, 0, 0, 0, 0, List.of(), null, null, null);
    }

    private Type type() {
      return type;
    }

    private int row() {
      return row;
    }

    private int column() {
      return column;
    }

    private int rowDelta() {
      return rowDelta;
    }

    private int columnDelta() {
      return columnDelta;
    }

    private List<Stone> chain() {
      return chain;
    }

    private String reason() {
      return reason;
    }

    private Cell intent() {
      return intent;
    }

    private Cell at() {
      return at;
    }
  }
}
