package com.remy.blockbattles.game.logic;

public enum TeamSide {
  RED("Red"),
  BLUE("Blue");

  private final String displayName;

  TeamSide(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public TeamSide otherSide() {
    return this == RED ? BLUE : RED;
  }
}
