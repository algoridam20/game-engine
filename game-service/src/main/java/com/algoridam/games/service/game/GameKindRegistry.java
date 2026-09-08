package com.algoridam.games.service.game;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GameKindRegistry {

  public static final String DEFAULT_GAME_TYPE = "SEVEN_EIGHT";

  private final Map<String, GameKind> kinds;

  public GameKindRegistry(List<GameKind> kinds) {
    this.kinds =
        kinds.stream().collect(Collectors.toUnmodifiableMap(GameKind::type, Function.identity()));
  }

  public GameKind require(String type) {
    GameKind kind = kinds.get(type);
    if (kind == null) {
      throw new ServiceException(ErrorCode.BAD_REQUEST, "Unknown game type: " + type);
    }
    return kind;
  }
}
