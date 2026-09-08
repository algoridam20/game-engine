package com.algoridam.games.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
