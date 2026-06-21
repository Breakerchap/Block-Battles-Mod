package com.remy.blockbattles.game.logic;

import java.util.List;
import java.util.Random;

public enum WitchHutEffect {
  NONE("None"),
  EXTRA_DRAW("Draw 4 Extra Blocks"),
  BREAK_AND_PLACE("Break 1 Block As Well"),
  DOUBLE_DAMAGE("Deal Double Damage"),
  NO_DAMAGE("Take No Damage"),
  DOUBLE_PLACEMENT("Place 2 Blocks"),
  PLACE_TIMER("Place In 5 Seconds"),
  REVIVE_ON_DEATH("Revive With 10 Health"),
  REMOVE_DECK_CARD("Remove 1 Deck Card"),
  SWAP_DECKS("Swap Decks"),
  NO_SUNLIGHT("No Sunlight");

  private static final List<WitchHutEffect> RANDOM_ROLLABLE = List.of(
      EXTRA_DRAW,
      BREAK_AND_PLACE,
      DOUBLE_DAMAGE,
      NO_DAMAGE,
      DOUBLE_PLACEMENT,
      REVIVE_ON_DEATH,
      REMOVE_DECK_CARD,
      SWAP_DECKS,
      NO_SUNLIGHT);

  private final String displayName;

  WitchHutEffect(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static WitchHutEffect randomRoll(Random random) {
    return RANDOM_ROLLABLE.get(random.nextInt(RANDOM_ROLLABLE.size()));
  }
}
