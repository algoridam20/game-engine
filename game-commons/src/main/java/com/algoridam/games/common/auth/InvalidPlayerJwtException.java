package com.algoridam.games.common.auth;

public class InvalidPlayerJwtException extends RuntimeException {

  public InvalidPlayerJwtException(String message) {
    super(message);
  }

  public InvalidPlayerJwtException(String message, Throwable cause) {
    super(message, cause);
  }
}
