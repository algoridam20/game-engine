package com.algoridam.games.common.auth;

import java.util.UUID;

public interface PlayerJwtIssuer {

  String generate(UUID playerId, String handle, String displayName, UUID gameId);
}
