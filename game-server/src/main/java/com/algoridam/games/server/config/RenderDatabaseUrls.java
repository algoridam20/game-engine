package com.algoridam.games.server.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record RenderDatabaseSettings(String url, String username, String password) {}

final class RenderDatabaseUrls {

  private static final Pattern POSTGRES =
      Pattern.compile("^postgres(?:ql)?://([^:]+):([^@]+)@([^:/]+)(?::(\\d+))?/([^?]+)(.*)$");

  private RenderDatabaseUrls() {}

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
    String query = matcher.group(6);
    if (query == null || query.isBlank()) {
      query = "?sslmode=require";
    } else if (!query.contains("sslmode=")) {
      query = query.contains("?") ? query + "&sslmode=require" : "?" + query + "&sslmode=require";
    }
    return new RenderDatabaseSettings(
        "jdbc:postgresql://" + host + ":" + port + "/" + database + query, username, password);
  }

  private static String decode(String value) {
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }
}
