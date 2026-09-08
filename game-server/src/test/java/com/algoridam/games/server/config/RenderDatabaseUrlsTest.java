package com.algoridam.games.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RenderDatabaseUrlsTest {

  @Test
  void fromPostgresUrl_convertsRenderInternalUrl() {
    RenderDatabaseSettings settings =
        RenderDatabaseUrls.fromPostgresUrl("postgresql://game:p%40ss@dpg-abc:5432/game_db");

    assertEquals("jdbc:postgresql://dpg-abc:5432/game_db?sslmode=require", settings.url());
    assertEquals("game", settings.username());
    assertEquals("p@ss", settings.password());
  }

  @Test
  void springDatasourceProperties_acceptsJdbcUrl() {
    Map<String, Object> properties =
        RenderDatabaseUrls.springDatasourceProperties("jdbc:postgresql://dpg-abc:5432/game_db")
            .orElseThrow();

    assertEquals(
        "jdbc:postgresql://dpg-abc:5432/game_db?sslmode=require",
        properties.get("spring.datasource.url"));
    assertEquals("org.postgresql.Driver", properties.get("spring.datasource.driver-class-name"));
  }
}
