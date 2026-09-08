package com.algoridam.games.service.model;

import java.util.UUID;

public record RoomSeat(UUID playerId, String handle, String displayName) {}
