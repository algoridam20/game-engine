package com.algoridam.games.server.controller;

import io.micrometer.observation.annotation.Observed;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

  @GetMapping
  @Observed(name = "health.check", contextualName = "checking-health")
  public Map<String, String> health() {
    return Map.of("status", "UP", "message", "Game Engine is running");
  }
}
