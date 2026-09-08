package com.algoridam.games.server.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (databaseUrl == null || databaseUrl.isBlank() || databaseUrl.startsWith("jdbc:")) {
      return;
    }
    RenderDatabaseSettings settings = RenderDatabaseUrls.fromPostgresUrl(databaseUrl);
    Map<String, Object> properties = new HashMap<>();
    properties.put("spring.datasource.url", settings.url());
    properties.put("spring.datasource.username", settings.username());
    properties.put("spring.datasource.password", settings.password());
    properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("renderDatabaseUrl", properties));
  }
}
