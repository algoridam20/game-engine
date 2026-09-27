package com.algoridam.games.mechakuchago.controller;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.mechakuchago.dto.LockMoveRequest;
import com.algoridam.games.mechakuchago.service.MechakuchaGoGameManager;
import com.algoridam.games.service.security.StompAuth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class MechakuchaGoRealtimeController {

  private final MechakuchaGoGameManager gameManager;

  @MessageMapping("/game/mechakucha_go/action/lock_move")
  public void lockMove(
      @Payload @Valid LockMoveRequest request, SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    gameManager.performActionLockMove(jwt.gameId(), jwt.playerId().toString(), request);
  }

  @MessageMapping("/game/mechakucha_go/action/next_round")
  public void nextRound(SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    gameManager.performActionInitNextRound(jwt.gameId());
  }

  @MessageMapping("/game/mechakucha_go/action/publish_state")
  public void publishState(SimpMessageHeaderAccessor headerAccessor) {
    PlayerJwtInfo jwt = StompAuth.requireGameJwt(headerAccessor);
    gameManager.publishGameState(jwt.gameId());
  }
}
