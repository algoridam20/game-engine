package com.algoridam.games.service.controller;

import com.algoridam.games.service.debug.StompTrafficLog;
import com.algoridam.games.service.debug.StompTrafficLog.Capture;
import com.algoridam.games.service.debug.StompTrafficLog.RoomTraffic;
import com.algoridam.games.service.model.GameRoom;
import com.algoridam.games.service.model.RoomSeat;
import com.algoridam.games.service.service.GameRoomManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/debug/stomp")
public class StompInspectorController {

  private final StompTrafficLog stompTrafficLog;
  private final GameRoomManager gameRoomManager;

  @GetMapping
  public Snapshot snapshot() {
    return new Snapshot(
        stompTrafficLog.destinations(),
        stompTrafficLog.snapshot(),
        stompTrafficLog.latestSnapshots(),
        rooms());
  }

  @DeleteMapping
  public void clear() {
    stompTrafficLog.clear();
  }

  private List<RoomView> rooms() {
    Map<String, RoomTraffic> traffic = stompTrafficLog.trafficByRoom();
    Map<String, RoomView> rooms = new LinkedHashMap<>();
    for (GameRoom room : gameRoomManager.listActiveRooms()) {
      String roomId = room.getRoomId().toString();
      rooms.put(roomId, toView(room, traffic.get(roomId)));
    }
    for (RoomTraffic roomTraffic : traffic.values()) {
      rooms.computeIfAbsent(roomTraffic.roomId, id -> ghostRoom(roomTraffic));
    }
    return new ArrayList<>(rooms.values());
  }

  private static RoomView toView(GameRoom room, RoomTraffic traffic) {
    List<String> handles =
        room.seatsInOrder().stream()
            .map(
                seat ->
                    seat.handle() == null || seat.handle().isBlank()
                        ? seat.displayName()
                        : seat.handle())
            .toList();
    List<String> topics = new ArrayList<>();
    topics.add("/topic/room/" + room.getRoomId());
    for (RoomSeat seat : room.seatsInOrder()) {
      topics.add("/topic/room/" + room.getRoomId() + "/player/" + seat.playerId());
    }
    if (traffic != null) {
      for (String topic : traffic.topics) {
        if (!topics.contains(topic)) {
          topics.add(topic);
        }
      }
    }
    int published = traffic == null ? 0 : traffic.published;
    int received = traffic == null ? 0 : traffic.received;
    return new RoomView(
        room.getRoomId().toString(),
        room.getGameType(),
        room.getStarted().get(),
        handles,
        topics,
        published,
        received,
        true);
  }

  private static RoomView ghostRoom(RoomTraffic traffic) {
    return new RoomView(
        traffic.roomId,
        "unknown",
        false,
        List.of(),
        List.copyOf(traffic.topics),
        traffic.published,
        traffic.received,
        false);
  }

  public record Snapshot(
      Set<String> channels, List<Capture> events, List<Capture> snapshots, List<RoomView> rooms) {}

  public record RoomView(
      String roomId,
      String gameType,
      boolean started,
      List<String> players,
      List<String> topics,
      int published,
      int received,
      boolean active) {}
}
