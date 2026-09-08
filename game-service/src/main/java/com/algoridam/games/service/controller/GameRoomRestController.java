package com.algoridam.games.service.controller;

import com.algoridam.games.common.dto.GeneralResponse;
import com.algoridam.games.service.dto.RoomDtos.CreateRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.JoinRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.RoomSessionResponse;
import com.algoridam.games.service.service.GameRoomManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GameRoomRestController {

  private final GameRoomManager gameRoomManager;

  @PostMapping("/room/create/init")
  public GeneralResponse<RoomSessionResponse, Object> createGameRoom(
      @RequestBody(required = false) CreateRoomRequest request) {
    log.info("createGameRoom");
    return GeneralResponse.ok(gameRoomManager.createRoom(request));
  }

  @PostMapping("/room/join/init")
  public GeneralResponse<RoomSessionResponse, Object> joinGameRoom(
      @Valid @RequestBody JoinRoomRequest request) {
    log.info("joinGameRoom");
    return GeneralResponse.ok(gameRoomManager.joinRoom(request));
  }
}
