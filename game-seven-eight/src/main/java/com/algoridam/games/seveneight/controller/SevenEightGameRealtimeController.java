package com.algoridam.games.seveneight.controller;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.service.security.StompAuth;
import com.algoridam.games.seveneight.dto.SevenEightCommonRequest;
import com.algoridam.games.seveneight.service.SevenEightGameManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class SevenEightGameRealtimeController {

  private final SevenEightGameManager sevenEightGameManager;

  @MessageMapping("/game/7_8/action/set_trump")
  public void setTrump(
      @Payload @Valid @NotNull final SevenEightCommonRequest request,
      final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    sevenEightGameManager.performActionSetTrump(
        jwt.gameId(), jwt.playerId().toString(), request.playedCard());
  }

  @MessageMapping("/game/7_8/action/play_card")
  public void playCard(
      @Payload @Valid @NotNull final SevenEightCommonRequest request,
      final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    sevenEightGameManager.performActionPlayCard(
        jwt.gameId(), jwt.playerId().toString(), request.playedCard());
  }

  @MessageMapping("/game/7_8/action/init_next_round")
  public void initNextRound(final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    sevenEightGameManager.performActionInitNextRound(jwt.gameId());
  }

  @MessageMapping("/game/7_8/action/publish_state")
  public void getGameState(final SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    sevenEightGameManager.publishGameState(jwt.gameId());
  }
}
