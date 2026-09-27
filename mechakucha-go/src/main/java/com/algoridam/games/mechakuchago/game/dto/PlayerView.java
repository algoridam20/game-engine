package com.algoridam.games.mechakuchago.game.dto;

import com.algoridam.games.mechakuchago.rules.FrameView;
import com.algoridam.games.mechakuchago.rules.StoneView;
import java.util.List;

public record PlayerView(
    String playerId,
    String handle,
    String color,
    String opponentHandle,
    String phase,
    int round,
    boolean youLocked,
    boolean opponentLocked,
    List<AxisView> legalAxes,
    List<StoneView> stones,
    List<FrameView> frames,
    int yourLines,
    int opponentLines,
    String winner,
    boolean draw,
    boolean gameOver) {}
