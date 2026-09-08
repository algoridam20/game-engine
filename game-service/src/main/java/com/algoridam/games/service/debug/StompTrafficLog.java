package com.algoridam.games.service.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompTrafficLog {

  public static final int MAX_EVENTS = 500;
  private static final Pattern ROOM_ID =
      Pattern.compile(
          "/topic/room/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

  public record Capture(
      Instant at,
      String direction,
      String command,
      String destination,
      String sessionId,
      String payload) {}

  private final ObjectMapper objectMapper;
  private final ConcurrentLinkedDeque<Capture> events = new ConcurrentLinkedDeque<>();
  private final ConcurrentHashMap<String, Capture> latestSnapshots = new ConcurrentHashMap<>();

  public void record(String direction, Message<?> message) {
    StompHeaderAccessor stomp =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
    if (messageType == SimpMessageType.HEARTBEAT) {
      return;
    }
    StompCommand command = stomp == null ? null : stomp.getCommand();
    String destination = destination(stomp, message);
    String sessionId =
        stomp != null && stomp.getSessionId() != null
            ? stomp.getSessionId()
            : stringHeader(message, SimpMessageHeaderAccessor.SESSION_ID_HEADER);
    if (command == null && destination.isBlank()) {
      return;
    }
    String commandName =
        command != null ? command.name() : (messageType == null ? "" : messageType.name());
    append(
        new Capture(
            Instant.now(),
            direction,
            commandName,
            destination,
            sessionId,
            payload(message.getPayload())));
  }

  public void recordPublish(String destination, Object payload) {
    Capture capture =
        new Capture(Instant.now(), "PUBLISH", "SNAPSHOT", destination, "", payload(payload));
    latestSnapshots.put(destination, capture);
    append(capture);
  }

  public List<Capture> snapshot() {
    List<Capture> copy = new ArrayList<>(events);
    copy.sort(Comparator.comparing(Capture::at));
    return copy;
  }

  public List<Capture> latestSnapshots() {
    return latestSnapshots.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .toList();
  }

  public Set<String> destinations() {
    Set<String> destinations = new LinkedHashSet<>();
    for (Capture capture : events) {
      if (!capture.destination().isBlank()) {
        destinations.add(capture.destination());
      }
    }
    return destinations;
  }

  public Map<String, RoomTraffic> trafficByRoom() {
    Map<String, RoomTraffic> byRoom = new LinkedHashMap<>();
    for (Capture capture : events) {
      String roomId = roomIdFrom(capture.destination());
      if (roomId == null) {
        continue;
      }
      RoomTraffic traffic = byRoom.computeIfAbsent(roomId, RoomTraffic::new);
      if (!capture.destination().isBlank()) {
        traffic.topics.add(capture.destination());
      }
      if ("IN".equals(capture.direction())) {
        traffic.received++;
      } else {
        traffic.published++;
      }
    }
    return byRoom;
  }

  public static String roomIdFrom(String destination) {
    if (destination == null || destination.isBlank()) {
      return null;
    }
    Matcher matcher = ROOM_ID.matcher(destination);
    return matcher.find() ? matcher.group(1) : null;
  }

  public static final class RoomTraffic {
    public final String roomId;
    public final Set<String> topics = new LinkedHashSet<>();
    public int published;
    public int received;

    private RoomTraffic(String roomId) {
      this.roomId = roomId;
    }
  }

  public void clear() {
    events.clear();
    latestSnapshots.clear();
  }

  private void append(Capture capture) {
    events.addLast(capture);
    while (events.size() > MAX_EVENTS) {
      events.pollFirst();
    }
  }

  private static String destination(StompHeaderAccessor stomp, Message<?> message) {
    if (stomp != null && stomp.getDestination() != null && !stomp.getDestination().isBlank()) {
      return stomp.getDestination();
    }
    return stringHeader(message, SimpMessageHeaderAccessor.DESTINATION_HEADER);
  }

  private static String stringHeader(Message<?> message, String header) {
    Object value = message.getHeaders().get(header);
    return value == null ? "" : String.valueOf(value);
  }

  private String payload(Object payload) {
    if (payload == null) {
      return "";
    }
    if (payload instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    if (payload instanceof String text) {
      return text;
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      return String.valueOf(payload);
    }
  }
}
