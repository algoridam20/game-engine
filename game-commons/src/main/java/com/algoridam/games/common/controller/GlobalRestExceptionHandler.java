package com.algoridam.games.common.controller;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.dto.ErrorObj;
import com.algoridam.games.common.dto.GeneralResponse;
import com.algoridam.games.common.exception.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<GeneralResponse<Object, Object>> handleIllegalArgumentException(
      IllegalArgumentException exception) {
    return getExceptionResponse(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.getMessage(), null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GeneralResponse<Object, Object>> handleValidationException(
      MethodArgumentNotValidException exception) {
    StringBuilder errorMessage = new StringBuilder();
    exception
        .getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              errorMessage.append(error.getDefaultMessage());
              errorMessage.append(", ");
            });
    if (!errorMessage.isEmpty()) {
      errorMessage.delete(errorMessage.length() - 2, errorMessage.length());
    }
    return getExceptionResponse(ErrorCode.BAD_REQUEST, errorMessage.toString(), null);
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<GeneralResponse<Object, Object>> handleServiceException(
      ServiceException exception) {
    return getExceptionResponse(
        exception.getErrorCode(),
        exception.getErrorCode().getMessage(),
        exception.getResponseContext());
  }

  private ResponseEntity<GeneralResponse<Object, Object>> getExceptionResponse(
      ErrorCode code, String errorMessage, Object errorContext) {
    return new ResponseEntity<>(
        GeneralResponse.builder()
            .httpStatus(code.getHttpStatus())
            .error(new ErrorObj<>(code.getName(), errorMessage, errorContext))
            .build(),
        code.getHttpStatus());
  }
}
