package com.algoridam.games.service.controller;

import com.algoridam.games.service.debug.StompTrafficLog;
import com.algoridam.games.service.debug.StompTrafficLog.Capture;
import java.util.List;
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

  @GetMapping
  public Snapshot snapshot() {
    return new Snapshot(
        stompTrafficLog.destinations(),
        stompTrafficLog.snapshot(),
        stompTrafficLog.latestSnapshots());
  }

  @DeleteMapping
  public void clear() {
    stompTrafficLog.clear();
  }

  public record Snapshot(Set<String> channels, List<Capture> events, List<Capture> snapshots) {}
}
