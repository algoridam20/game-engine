package com.algoridam.games.mechakuchago.rules;

public record StoneView(
    String id, int row, int column, String color, boolean moving, boolean dying) {}
