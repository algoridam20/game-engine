package com.algoridam.games.mechakuchago.rules;

import static com.algoridam.games.mechakuchago.rules.Constants.BOARD_SIZE;
import static com.algoridam.games.mechakuchago.rules.Constants.MAX_PUSH_DECISIONS;
import static com.algoridam.games.mechakuchago.rules.Constants.MAX_SLIDE_FRAMES;

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
    List<Stone> stones = stonesAlreadyOn(board, reuseIds);
    List<String> ignoredMoves = new ArrayList<>();
    placeIncomingStone(board, stones, black, ignoredMoves);
    placeIncomingStone(board, stones, white, ignoredMoves);

    List<FrameView> frames = new ArrayList<>();
    frames.add(snapshot(stones, openingEvents(ignoredMoves)));
    for (int frame = 0; frame < MAX_SLIDE_FRAMES; frame++) {
      if (!slideOneFrame(stones, frames)) {
        break;
      }
    }
    appendSettleFrameIfAStoneIsDying(stones, frames);
    return new Resolution(boardFrom(stones), List.copyOf(frames));
  }

  private List<Stone> stonesAlreadyOn(Board board, Map<String, String> reuseIds) {
    List<Stone> stones = new ArrayList<>();
    for (int row = 0; row < BOARD_SIZE; row++) {
      for (int column = 0; column < BOARD_SIZE; column++) {
        Color color = board.at(row, column);
        if (color == null) {
          continue;
        }
        String key = row + "," + column;
        String id = reuseIds.getOrDefault(key, "s" + sequence++);
        stones.add(Stone.sitting(id, color, row, column));
      }
    }
    return stones;
  }

  private void placeIncomingStone(
      Board board, List<Stone> stones, Move move, List<String> ignoredMoves) {
    if (move == null) {
      return;
    }
    if (board.axisFull(move.direction(), move.index())) {
      ignoredMoves.add(move.player().label() + " chose a full line. The move is ignored.");
      return;
    }
    stones.add(Stone.mover("m" + sequence++, move));
  }

  private static List<String> openingEvents(List<String> ignoredMoves) {
    List<String> events = new ArrayList<>(ignoredMoves);
    events.add("Tiles appear at the edge and start sliding.");
    return events;
  }

  private static boolean slideOneFrame(List<Stone> stones, List<FrameView> frames) {
    List<Stone> moving = stonesStillMoving(stones);
    if (moving.isEmpty()) {
      return false;
    }
    Map<String, Plan> plans = new HashMap<>();
    List<String> events = new ArrayList<>();
    markStonesThatReachedTheirStop(moving, plans);
    List<Stone> stillMoving = stonesWithoutAPlan(moving, plans);
    if (stillMoving.size() == 2) {
      stopHeadOnOrDestroyCrossingPair(
          stones, stillMoving.get(0), stillMoving.get(1), plans, events);
    }
    List<Stone> waiting = stonesWithoutAPlan(stillMoving, plans);
    decideWaitingStones(stones, waiting, plans);
    stopIfStillUndecided(waiting, plans);
    stopIfBothShoveTheSameLine(stones, plans, events);
    applyPlans(stones, plans, events);
    frames.add(snapshot(stones, events));
    stones.removeIf(stone -> !stone.alive());
    return true;
  }

  private static List<Stone> stonesStillMoving(List<Stone> stones) {
    List<Stone> moving = new ArrayList<>();
    for (Stone stone : stones) {
      if (stone.alive() && stone.moving()) {
        moving.add(stone);
      }
    }
    return moving;
  }

  private static void markStonesThatReachedTheirStop(List<Stone> moving, Map<String, Plan> plans) {
    for (Stone stone : moving) {
      if (stone.arrived()) {
        plans.put(stone.id(), Plan.stay("arrived", new Cell(stone.row(), stone.column())));
      }
    }
  }

  private static List<Stone> stonesWithoutAPlan(List<Stone> stones, Map<String, Plan> plans) {
    List<Stone> waiting = new ArrayList<>();
    for (Stone stone : stones) {
      if (!plans.containsKey(stone.id())) {
        waiting.add(stone);
      }
    }
    return waiting;
  }

  private static void stopHeadOnOrDestroyCrossingPair(
      List<Stone> stones, Stone first, Stone second, Map<String, Plan> plans, List<String> events) {
    Cell firstNext = first.next();
    Cell secondNext = second.next();
    boolean swappingPlaces =
        firstNext.row() == second.row()
            && firstNext.column() == second.column()
            && secondNext.row() == first.row()
            && secondNext.column() == first.column();
    boolean enteringTheSameSquare =
        firstNext.row() == secondNext.row()
            && firstNext.column() == secondNext.column()
            && Board.inBounds(firstNext.row(), firstNext.column());
    if (swappingPlaces) {
      plans.put(first.id(), Plan.stay("headon", new Cell(first.row(), first.column())));
      plans.put(second.id(), Plan.stay("headon", new Cell(second.row(), second.column())));
      events.add("The tiles meet face to face in adjacent squares. Both lines stop.");
      return;
    }
    if (enteringTheSameSquare
        && stoneAt(stones, firstNext.row(), firstNext.column(), null) == null) {
      plans.put(first.id(), Plan.die(firstNext));
      plans.put(second.id(), Plan.die(secondNext));
      events.add(
          "Both tiles enter "
              + Board.cellName(firstNext.row(), firstNext.column())
              + " on the same frame and destroy each other.");
    }
  }

  private static void decideWaitingStones(
      List<Stone> stones, List<Stone> waiting, Map<String, Plan> plans) {
    int guard = 0;
    while (aStoneIsStillUndecided(waiting, plans) && guard < MAX_PUSH_DECISIONS) {
      guard++;
      for (Stone stone : orderSoTheBlockingStoneIsDecidedFirst(waiting)) {
        Plan existing = plans.get(stone.id());
        if (existing != null && existing.type() != Plan.Type.RETRY) {
          continue;
        }
        plans.put(stone.id(), planForStone(stone, stones, plans));
      }
    }
  }

  private static boolean aStoneIsStillUndecided(List<Stone> waiting, Map<String, Plan> plans) {
    for (Stone stone : waiting) {
      Plan plan = plans.get(stone.id());
      if (plan == null || plan.type() == Plan.Type.RETRY) {
        return true;
      }
    }
    return false;
  }

  private static List<Stone> orderSoTheBlockingStoneIsDecidedFirst(List<Stone> waiting) {
    List<Stone> ordered = new ArrayList<>(waiting);
    ordered.sort(
        (stone, other) -> {
          Cell stoneNext = stone.next();
          boolean stoneHitsOther =
              stoneNext.row() == other.row() && stoneNext.column() == other.column();
          Cell otherNext = other.next();
          boolean otherHitsStone =
              otherNext.row() == stone.row() && otherNext.column() == stone.column();
          if (stoneHitsOther && !otherHitsStone) {
            return 1;
          }
          if (otherHitsStone && !stoneHitsOther) {
            return -1;
          }
          return 0;
        });
    return ordered;
  }

  private static void stopIfStillUndecided(List<Stone> waiting, Map<String, Plan> plans) {
    for (Stone stone : waiting) {
      Plan plan = plans.get(stone.id());
      if (plan == null || plan.type() == Plan.Type.RETRY) {
        plans.put(stone.id(), Plan.stay("blocked", new Cell(stone.row(), stone.column())));
      }
    }
  }

  private static Plan planForStone(Stone stone, List<Stone> stones, Map<String, Plan> plans) {
    Cell next = stone.next();
    if (!Board.inBounds(next.row(), next.column())) {
      return Plan.stay("blocked", next);
    }
    Stone occupied = stoneAt(stones, next.row(), next.column(), stone.id());
    if (occupied == null) {
      return Plan.move(next);
    }
    if (occupied.moving()) {
      Plan other = plans.get(occupied.id());
      if (other == null || other.type() == Plan.Type.RETRY) {
        return Plan.retry();
      }
      if (other.type() == Plan.Type.STAY) {
        return shoveOrStop(stone, stones, next, plans);
      }
      return Plan.move(next);
    }
    return shoveOrStop(stone, stones, next, plans);
  }

  private static Plan shoveOrStop(
      Stone stone, List<Stone> stones, Cell hit, Map<String, Plan> plans) {
    Chain line = lineInFront(stones, hit, stone.rowDelta(), stone.columnDelta(), plans);
    if (line.stones().isEmpty()) {
      return Plan.move(hit);
    }
    if (!Board.inBounds(line.beyond().row(), line.beyond().column())) {
      return Plan.stay("blocked", new Cell(stone.row(), stone.column()));
    }
    Stone blocker = stoneAt(stones, line.beyond().row(), line.beyond().column(), null);
    if (blocker != null) {
      if (!blocker.moving()) {
        return Plan.stay("blocked", new Cell(stone.row(), stone.column()));
      }
      Plan other = plans.get(blocker.id());
      if (other == null || other.type() == Plan.Type.RETRY) {
        Cell otherNext = blocker.next();
        boolean aimingIntoLine =
            line.stones().stream()
                .anyMatch(
                    sitting ->
                        sitting.row() == otherNext.row() && sitting.column() == otherNext.column());
        if (!aimingIntoLine) {
          return Plan.retry();
        }
      } else if (other.type() == Plan.Type.STAY || other.type() == Plan.Type.DIE) {
        return Plan.stay("blocked", new Cell(stone.row(), stone.column()));
      }
    }
    return Plan.push(hit, stone.rowDelta(), stone.columnDelta(), line.stones());
  }

  private static Chain lineInFront(
      List<Stone> stones, Cell hit, int rowDelta, int columnDelta, Map<String, Plan> plans) {
    List<Stone> line = new ArrayList<>();
    int row = hit.row();
    int column = hit.column();
    while (Board.inBounds(row, column)) {
      Stone occupied = stoneAt(stones, row, column, null);
      if (occupied == null) {
        break;
      }
      if (occupied.moving()) {
        Plan plan = plans.get(occupied.id());
        if (plan == null || plan.type() != Plan.Type.STAY) {
          break;
        }
      }
      line.add(occupied);
      row += rowDelta;
      column += columnDelta;
    }
    return new Chain(line, new Cell(row, column));
  }

  private static void stopIfBothShoveTheSameLine(
      List<Stone> stones, Map<String, Plan> plans, List<String> events) {
    List<Map.Entry<String, Plan>> shoves = new ArrayList<>();
    for (Map.Entry<String, Plan> entry : plans.entrySet()) {
      if (entry.getValue().type() == Plan.Type.PUSH) {
        shoves.add(entry);
      }
    }
    if (shoves.size() != 2) {
      return;
    }
    Set<String> firstLine = new HashSet<>();
    for (Stone stone : shoves.get(0).getValue().chain()) {
      firstLine.add(stone.id());
    }
    boolean sameLine = false;
    for (Stone stone : shoves.get(1).getValue().chain()) {
      if (firstLine.contains(stone.id())) {
        sameLine = true;
        break;
      }
    }
    if (!sameLine) {
      return;
    }
    Stone first = stoneById(stones, shoves.get(0).getKey());
    Stone second = stoneById(stones, shoves.get(1).getKey());
    plans.put(first.id(), Plan.stay("lock", new Cell(first.row(), first.column())));
    plans.put(second.id(), Plan.stay("lock", new Cell(second.row(), second.column())));
    events.add("Both tiles shove the same line on this frame. The line locks and both stop.");
  }

  private static void applyPlans(List<Stone> stones, Map<String, Plan> plans, List<String> events) {
    List<Shift> shifts = shiftsFor(stones, plans, shovedStones(plans));
    Set<String> destroyed = stonesEnteringTheSameSquare(shifts, events);
    destroyStonesBlockedByASittingStone(stones, shifts, plans, destroyed, events);
    relocateSurvivors(shifts, destroyed, events);
    finishStonesThatStayOrDie(stones, plans, events);
  }

  private static Map<String, Plan> shovedStones(Map<String, Plan> plans) {
    Map<String, Plan> shoved = new HashMap<>();
    for (Plan plan : plans.values()) {
      if (plan.type() != Plan.Type.PUSH) {
        continue;
      }
      for (Stone stone : plan.chain()) {
        shoved.put(stone.id(), plan);
      }
    }
    return shoved;
  }

  private static List<Shift> shiftsFor(
      List<Stone> stones, Map<String, Plan> plans, Map<String, Plan> shoved) {
    List<Shift> shifts = new ArrayList<>();
    for (Stone stone : stones) {
      if (!stone.alive()) {
        continue;
      }
      Plan plan = plans.get(stone.id());
      if (plan != null && plan.type() == Plan.Type.DIE) {
        continue;
      }
      if (plan != null && plan.type() == Plan.Type.MOVE) {
        shifts.add(new Shift(stone, plan.row(), plan.column(), Motion.SLIDE, plan));
        continue;
      }
      if (plan != null && plan.type() == Plan.Type.PUSH) {
        shifts.add(new Shift(stone, plan.row(), plan.column(), Motion.SHOVE, plan));
        continue;
      }
      Plan shove = shoved.get(stone.id());
      if (shove != null) {
        shifts.add(
            new Shift(
                stone,
                stone.row() + shove.rowDelta(),
                stone.column() + shove.columnDelta(),
                Motion.CARRIED,
                shove));
      }
    }
    return shifts;
  }

  private static Set<String> stonesEnteringTheSameSquare(List<Shift> shifts, List<String> events) {
    Map<String, List<Shift>> groups = new HashMap<>();
    for (Shift shift : shifts) {
      groups
          .computeIfAbsent(shift.row() + "," + shift.column(), key -> new ArrayList<>())
          .add(shift);
    }
    Set<String> destroyed = new HashSet<>();
    for (List<Shift> group : groups.values()) {
      if (group.size() <= 1) {
        continue;
      }
      Shift sample = group.get(0);
      events.add(
          "Two tiles both enter "
              + Board.cellName(sample.row(), sample.column())
              + " and are destroyed.");
      for (Shift shift : group) {
        destroyed.add(shift.stone().id());
      }
    }
    return destroyed;
  }

  private static void destroyStonesBlockedByASittingStone(
      List<Stone> stones,
      List<Shift> shifts,
      Map<String, Plan> plans,
      Set<String> destroyed,
      List<String> events) {
    Set<String> sitting = new HashSet<>();
    for (Stone stone : stones) {
      if (!stone.alive() || destroyed.contains(stone.id())) {
        continue;
      }
      if (shifts.stream().anyMatch(shift -> shift.stone().id().equals(stone.id()))) {
        continue;
      }
      Plan plan = plans.get(stone.id());
      if (plan != null && plan.type() == Plan.Type.DIE) {
        continue;
      }
      if (Board.inBounds(stone.row(), stone.column())) {
        sitting.add(stone.row() + "," + stone.column());
      }
    }
    for (Shift shift : shifts) {
      if (destroyed.contains(shift.stone().id())) {
        continue;
      }
      if (sitting.contains(shift.row() + "," + shift.column())) {
        destroyed.add(shift.stone().id());
        events.add(
            shift.stone().color().label()
                + " cannot enter "
                + Board.cellName(shift.row(), shift.column())
                + " and is destroyed.");
      }
    }
  }

  private static void relocateSurvivors(
      List<Shift> shifts, Set<String> destroyed, List<String> events) {
    for (Shift shift : shifts) {
      if (destroyed.contains(shift.stone().id())) {
        shift.stone().destroyAt(shift.row(), shift.column());
        continue;
      }
      shift.stone().relocate(shift.row(), shift.column());
      if (shift.motion() == Motion.SLIDE) {
        boolean arrived = shift.stone().arrived();
        if (arrived) {
          shift.stone().halt();
        }
        events.add(
            shift.stone().color().label()
                + " slides to "
                + Board.cellName(shift.stone().row(), shift.stone().column())
                + (arrived ? " and stops." : "."));
      } else if (shift.motion() == Motion.SHOVE) {
        shift.stone().halt();
        events.add(
            shift.stone().color().label()
                + " shoves a line of "
                + shift.plan().chain().size()
                + " one square and stops at "
                + Board.cellName(shift.stone().row(), shift.stone().column())
                + ".");
      } else if (shift.stone().moving()) {
        shift.stone().halt();
      }
    }
  }

  private static void finishStonesThatStayOrDie(
      List<Stone> stones, Map<String, Plan> plans, List<String> events) {
    for (Map.Entry<String, Plan> entry : plans.entrySet()) {
      Plan plan = entry.getValue();
      if (plan.type() != Plan.Type.DIE && plan.type() != Plan.Type.STAY) {
        continue;
      }
      Stone stone = stoneById(stones, entry.getKey());
      if (stone == null) {
        continue;
      }
      if (plan.type() == Plan.Type.DIE) {
        stone.destroyAt(plan.at().row(), plan.at().column());
        continue;
      }
      stone.halt();
      if (!Board.inBounds(stone.row(), stone.column())) {
        if (plan.intent() != null && Board.inBounds(plan.intent().row(), plan.intent().column())) {
          stone.relocate(plan.intent().row(), plan.intent().column());
        }
        stone.destroyAt(stone.row(), stone.column());
        events.add(stone.color().label() + " cannot enter the line and is lost.");
      } else if ("blocked".equals(plan.reason())) {
        events.add(
            stone.color().label()
                + " is packed against the edge and stops at "
                + Board.cellName(stone.row(), stone.column())
                + ".");
      }
    }
  }

  private static void appendSettleFrameIfAStoneIsDying(List<Stone> stones, List<FrameView> frames) {
    boolean aStoneIsDying =
        frames.get(frames.size() - 1).stones().stream().anyMatch(StoneView::dying);
    if (aStoneIsDying) {
      frames.add(snapshot(stones, List.of("The board settles.")));
    }
  }

  private static FrameView snapshot(List<Stone> stones, List<String> events) {
    List<StoneView> views = new ArrayList<>();
    for (Stone stone : stones) {
      if (!stone.alive() && !stone.dying()) {
        continue;
      }
      boolean visible =
          stone.dying() || stone.moving() || Board.inBounds(stone.row(), stone.column());
      if (visible) {
        views.add(stone.view());
      }
    }
    return new FrameView(List.copyOf(events), List.copyOf(views));
  }

  private static Board boardFrom(List<Stone> stones) {
    Board board = Board.empty();
    for (Stone stone : stones) {
      if (stone.alive() && Board.inBounds(stone.row(), stone.column())) {
        board.place(stone.row(), stone.column(), stone.color());
      }
    }
    return board;
  }

  private static Stone stoneAt(List<Stone> stones, int row, int column, String ignoreId) {
    for (Stone stone : stones) {
      if (!stone.alive() || stone.id().equals(ignoreId)) {
        continue;
      }
      if (stone.row() == row && stone.column() == column) {
        return stone;
      }
    }
    return null;
  }

  private static Stone stoneById(List<Stone> stones, String id) {
    for (Stone stone : stones) {
      if (stone.id().equals(id)) {
        return stone;
      }
    }
    return null;
  }

  public record Resolution(Board board, List<FrameView> frames) {}

  private record Chain(List<Stone> stones, Cell beyond) {}

  private enum Motion {
    SLIDE,
    SHOVE,
    CARRIED
  }

  private record Shift(Stone stone, int row, int column, Motion motion, Plan plan) {}

  private record Plan(
      Type type,
      int row,
      int column,
      int rowDelta,
      int columnDelta,
      List<Stone> chain,
      String reason,
      Cell intent,
      Cell at) {

    private enum Type {
      MOVE,
      PUSH,
      STAY,
      DIE,
      RETRY
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
  }
}
