package com.algoridam.games.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  DUPLICATE_REQUEST("DUPLICATE_REQUEST", "Resource already exists", HttpStatus.BAD_REQUEST),
  BAD_REQUEST("BAD_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),
  DATA_NOT_FOUND("DATA_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
  ROOM_FULL("ROOM_FULL", "Room is full", HttpStatus.BAD_REQUEST),
  UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("FORBIDDEN", "Operation is not allowed", HttpStatus.FORBIDDEN),
  CANNOT_DELETE_LAST_PASSKEY(
      "CANNOT_DELETE_LAST_PASSKEY", "At least one passkey must remain", HttpStatus.BAD_REQUEST),
  PASSKEY_VERIFICATION_FAILED(
      "PASSKEY_VERIFICATION_FAILED", "Passkey verification failed", HttpStatus.UNAUTHORIZED);

  private final String name;
  private final String message;
  private final HttpStatus httpStatus;
}
