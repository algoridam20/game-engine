package com.algoridam.games.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorObj<C> {
  private final String code;
  private final String message;
  private final C context;
}
