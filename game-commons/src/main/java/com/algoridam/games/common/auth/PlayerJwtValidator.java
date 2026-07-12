package com.algoridam.games.common.auth;

public interface PlayerJwtValidator {

  PlayerJwtInfo validate(String token);
}
