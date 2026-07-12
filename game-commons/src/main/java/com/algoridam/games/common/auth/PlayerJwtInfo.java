package com.algoridam.games.common.auth;

import java.time.Instant;
import java.util.UUID;

public record PlayerJwtInfo(
    int tokenVersion,
    UUID playerId,
    String handle,
    String displayName,
    UUID gameId,
    Instant issuedAt,
    Instant expiresAt) {}
