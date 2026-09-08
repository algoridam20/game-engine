package com.algoridam.games.server.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl =
        firstNonBlank(
            environment.getProperty("DATABASE_URL"),
            environment.getProperty("POSTGRES_URL"),
            System.getenv("DATABASE_URL"),
            System.getenv("POSTGRES_URL"));
    RenderDatabaseUrls.springDatasourceProperties(databaseUrl)
        .ifPresent(
            properties -> {
              environment
                  .getPropertySources()
                  .addFirst(new MapPropertySource("renderDatabaseUrl", properties));
              System.out.println(
                  "Using PostgreSQL datasource url=" + properties.get("spring.datasource.url"));
            });
    if (onRender(environment) && !usingPostgres(environment, databaseUrl)) {
      throw new IllegalStateException(
          "Render is using the local MySQL default. Set DATABASE_URL to the Internal Database URL"
              + " (postgresql://USER:PASSWORD@host:5432/dbname). Username and password alone are not"
              + " enough.");
    }
  }

  private static boolean usingPostgres(ConfigurableEnvironment environment, String databaseUrl) {
    Object url = environment.getProperty("spring.datasource.url");
    String resolved = url == null ? databaseUrl : url.toString();
    return resolved != null && resolved.contains("postgres");
  }

  private static boolean onRender(ConfigurableEnvironment environment) {
    return "true"
        .equalsIgnoreCase(
            firstNonBlank(environment.getProperty("RENDER"), System.getenv("RENDER")));
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
