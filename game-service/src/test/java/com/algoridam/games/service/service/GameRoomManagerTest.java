package com.algoridam.games.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoridam.games.common.auth.PlayerJwtInfo;
import com.algoridam.games.common.auth.PlayerJwtIssuer;
import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.service.dto.RoomDtos.CreateRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.JoinRoomRequest;
import com.algoridam.games.service.dto.RoomDtos.RoomSessionResponse;
import com.algoridam.games.service.game.GameKind;
import com.algoridam.games.service.game.GameKindRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class GameRoomManagerTest {

  private static final String HANDLE = "alice";
  private static final String DISPLAY_NAME = "Alice";

  @Mock private GameKind gameKind;
  @Mock private PlayerJwtIssuer playerJwtIssuer;
  @Mock private SimpMessageSendingOperations messageTemplate;

  private GameRoomManager gameRoomManager;

  @BeforeEach
  void setUp() {
    lenient().when(gameKind.type()).thenReturn(GameKindRegistry.DEFAULT_GAME_TYPE);
    lenient().when(gameKind.maxPlayers()).thenReturn(2);
    gameRoomManager =
        new GameRoomManager(
            new GameKindRegistry(List.of(gameKind)), playerJwtIssuer, messageTemplate);
    lenient()
        .when(playerJwtIssuer.generate(any(), any(), any(), any()))
        .thenAnswer(invocation -> "token-" + invocation.getArgument(3));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRoom_issuesGameScopedJwt() {
    UUID playerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    authenticate(playerId, null);

    RoomSessionResponse response = gameRoomManager.createRoom(null);

    verify(playerJwtIssuer)
        .generate(eq(playerId), eq(HANDLE), eq(DISPLAY_NAME), eq(response.roomId()));
    assertEquals("token-" + response.roomId(), response.token());
    assertEquals(
        playerId, gameRoomManager.getRoomForTests(response.roomId()).getSeats().get(0).playerId());
  }

  @Test
  void createRoom_unknownGameType_isRejected() {
    authenticate(UUID.randomUUID(), null);

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> gameRoomManager.createRoom(new CreateRoomRequest("UNKNOWN")));
    assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
  }

  @Test
  void joinRoom_isIdempotentForSamePlayer() {
    UUID playerId = UUID.randomUUID();
    authenticate(playerId, null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);

    RoomSessionResponse joined = gameRoomManager.joinRoom(new JoinRoomRequest(created.roomId()));

    assertEquals(created.roomId(), joined.roomId());
    assertEquals(1, gameRoomManager.getRoomForTests(created.roomId()).getSeats().size());
  }

  @Test
  void joinRoom_thirdPlayer_isRoomFull() {
    UUID creator = UUID.randomUUID();
    UUID joiner = UUID.randomUUID();
    UUID extra = UUID.randomUUID();
    authenticate(creator, null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);

    authenticate(joiner, null);
    gameRoomManager.joinRoom(new JoinRoomRequest(created.roomId()));

    authenticate(extra, null);
    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> gameRoomManager.joinRoom(new JoinRoomRequest(created.roomId())));
    assertEquals(ErrorCode.ROOM_FULL, exception.getErrorCode());
  }

  @Test
  void joinRealtime_startsGameOnceWhenSeatsReady() {
    when(gameKind.minPlayers()).thenReturn(2);
    UUID creator = UUID.randomUUID();
    UUID joiner = UUID.randomUUID();
    authenticate(creator, null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);
    authenticate(joiner, null);
    gameRoomManager.joinRoom(new JoinRoomRequest(created.roomId()));

    gameRoomManager.joinRealtime(jwt(joiner, created.roomId()));
    gameRoomManager.joinRealtime(jwt(creator, created.roomId()));

    verify(gameKind).onSeatsReady(eq(created.roomId()), any());
    verify(gameKind).publishState(created.roomId());
  }

  @Test
  void joinRealtime_doesNotStartTwice() {
    when(gameKind.minPlayers()).thenReturn(2);
    UUID creator = UUID.randomUUID();
    UUID joiner = UUID.randomUUID();
    authenticate(creator, null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);
    authenticate(joiner, null);
    gameRoomManager.joinRoom(new JoinRoomRequest(created.roomId()));

    PlayerJwtInfo joinerJwt = jwt(joiner, created.roomId());
    gameRoomManager.joinRealtime(joinerJwt);
    gameRoomManager.joinRealtime(joinerJwt);

    verify(gameKind).onSeatsReady(eq(created.roomId()), any());
  }

  @Test
  void joinRealtime_playerNotSeated_isNotFound() {
    authenticate(UUID.randomUUID(), null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> gameRoomManager.joinRealtime(jwt(UUID.randomUUID(), created.roomId())));
    assertEquals(ErrorCode.DATA_NOT_FOUND, exception.getErrorCode());
    verify(gameKind, never()).onSeatsReady(any(), any());
    verify(gameKind, never()).publishState(any());
  }

  @Test
  void getRoomForTests_returnsSameInstance() {
    authenticate(UUID.randomUUID(), null);
    RoomSessionResponse created = gameRoomManager.createRoom(null);
    assertSame(
        gameRoomManager.getRoomForTests(created.roomId()),
        gameRoomManager.getRoomForTests(created.roomId()));
  }

  private void authenticate(UUID playerId, UUID gameId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                jwt(playerId, gameId), null, AuthorityUtils.NO_AUTHORITIES));
  }

  private PlayerJwtInfo jwt(UUID playerId, UUID gameId) {
    Instant now = Instant.now();
    return new PlayerJwtInfo(1, playerId, HANDLE, DISPLAY_NAME, gameId, now, now.plusSeconds(3600));
  }
}
