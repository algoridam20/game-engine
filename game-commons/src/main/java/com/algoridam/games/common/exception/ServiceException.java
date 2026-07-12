package com.algoridam.games.common.exception;

import com.algoridam.games.common.dto.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ServiceException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Object responseContext;

  public ServiceException(ErrorCode code) {
    super(code.getMessage());
    this.errorCode = code;
    this.responseContext = null;
    log.error(code.getName());
  }

  public ServiceException(ErrorCode code, String loggingContext) {
    super(code.getMessage());
    this.errorCode = code;
    this.responseContext = null;
    log.error(loggingContext);
  }

  public ServiceException(
      final Exception exp,
      final ErrorCode code,
      final Object loggingContext,
      final Object responseContext) {
    super(code.getMessage());
    this.errorCode = code;
    this.responseContext = responseContext;
    log.error("ErrorCode: {}, Context: {}", code.getName(), loggingContext, exp);
  }
}
