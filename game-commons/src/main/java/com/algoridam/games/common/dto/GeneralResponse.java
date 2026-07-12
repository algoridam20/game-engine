package com.algoridam.games.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
public class GeneralResponse<D, C> {
  private final HttpStatus httpStatus;
  private final D data;
  private final ErrorObj<C> error;

  public static <D> GeneralResponse<D, Object> ok(D data) {
    return GeneralResponse.<D, Object>builder().httpStatus(HttpStatus.OK).data(data).build();
  }
}
