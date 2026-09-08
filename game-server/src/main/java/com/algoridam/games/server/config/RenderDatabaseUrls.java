package com.algoridam.games.server.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record RenderDatabaseSettings(String url, String username, String password) {}

final class RenderDatabaseUrls {

  private static final Pattern POSTGRES =
      Pattern.compile("^postgres(?:ql)?://([^:]+):([^@]+)@([^:/]+)(?::(\\d+))?/([^?]+)(.*)$");

  private RenderDatabaseUrls() {}

  static Optional<Map<String, Object>> springDatasourceProperties(String databaseUrl) {
    if (databaseUrl == null || databaseUrl.isBlank()) {
      return Optional.empty();
    }
    String trimmed = databaseUrl.trim();
    Map<String, Object> properties = new LinkedHashMap<>();
    if (trimmed.startsWith("jdbc:postgresql:")) {
      properties.put("spring.datasource.url", withSslMode(trimmed));
      properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
      return Optional.of(properties);
    }
    if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
      return Optional.empty();
    }
    RenderDatabaseSettings settings = fromPostgresUrl(trimmed);
    properties.put("spring.datasource.url", settings.url());
    properties.put("spring.datasource.username", settings.username());
    properties.put("spring.datasource.password", settings.password());
    properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
    return Optional.of(properties);
  }

  static RenderDatabaseSettings fromPostgresUrl(String databaseUrl) {
    Matcher matcher = POSTGRES.matcher(databaseUrl.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Unsupported DATABASE_URL: " + databaseUrl);
    }
    String username = decode(matcher.group(1));
    String password = decode(matcher.group(2));
    String host = matcher.group(3);
    String port = matcher.group(4) == null ? "5432" : matcher.group(4);
    String database = matcher.group(5);
    String query = withSslMode(matcher.group(6) == null ? "" : matcher.group(6));
    if (!query.startsWith("?")) {
      query = query.isBlank() ? "?sslmode=require" : "?" + query;
    }
    return new RenderDatabaseSettings(
        "jdbc:postgresql://" + host + ":" + port + "/" + database + query, username, password);
  }

  private static String withSslMode(String urlOrQuery) {
    if (urlOrQuery.contains("sslmode=")) {
      return urlOrQuery;
    }
    if (urlOrQuery.isBlank()) {
      return "?sslmode=require";
    }
    return urlOrQuery.contains("?")
        ? urlOrQuery + "&sslmode=require"
        : urlOrQuery + "?sslmode=require";
  }

  private static String decode(String value) {
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }
}
