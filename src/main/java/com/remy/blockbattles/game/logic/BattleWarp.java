package com.remy.blockbattles.game.logic;

import com.remy.blockbattles.BlockBattlesMod;

import net.minecraft.resources.Identifier;

public enum BattleWarp {
  NONE("None", null, null),
  NIGHT("Night Warp", false, "night_warp"),
  TRIAL_CHAMBER("Trial Chamber Warp", null, "trial_chamber_warp"),
  VILLAGE_HOUSE("Village House Warp", true, "village_house_warp"),
  END("End Warp", false, "end_warp"),
  LIBRARY("Library Warp", false, "library_warp"),
  WITCH_HUT("Witch Hut Warp", null, "witch_hut_warp"),
  DEEP_DARK("Deep Dark Warp", false, "deep_dark_warp"),
  SOUL_SAND_VALLEY("Soul Sand Valley Warp", false, "soul_sand_valley_warp"),
  BLIZZARD("Blizzard Warp", false, "blizzard_warp"),
  OCEAN("Ocean Warp", true, "ocean_warp"),
  REDSTONE("Redstone Warp", true, "redstone_warp"),
  NETHER_STRIP_MINE("Nether Strip Mine Warp", false, "nether_strip_mine_warp"),
  NETHER("Nether Warp", false, "nether_warp"),
  LUSH_CAVE("Lush Cave Warp", false, "lush_cave_warp"),
  DESERT("Desert Warp", true, "desert_warp"),
  STRIP_MINE("Strip Mine Warp", true, "strip_mine_warp"),
  BED_WARS("Bed Wars Warp", true, "bed_wars_warp"),
  SWAMP("Swamp Warp", true, "swamp_warp"),
  DRIPSTONE_CAVE("Dripstone Cave Warp", true, "dripstone_cave_warp"),
  NETHER_FORTRESS("Nether Fortress Warp", false, "nether_fortress_warp"),
  BASTION("Bastion Warp", false, "bastion_warp"),
  FLOWER_FOREST("Flower Forest Warp", false, "flower_forest_warp"),
  CAVE("Cave Warp", false, "cave_warp"),
  MESA("Mesa Warp", true, "mesa_warp"),
  PALE_GARDEN("Pale Garden Warp", false, "pale_garden_warp");

  private final String displayName;
  private final Boolean sunlightOverride;
  private final String structurePath;

  BattleWarp(String displayName, Boolean sunlightOverride, String structurePath) {
    this.displayName = displayName;
    this.sunlightOverride = sunlightOverride;
    this.structurePath = structurePath;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean overridesSunlight() {
    return sunlightOverride != null;
  }

  public boolean hasSunlight() {
    return Boolean.TRUE.equals(sunlightOverride);
  }

  public Identifier getStructureId() {
    if (structurePath == null || structurePath.isBlank()) {
      return null;
    }

    return Identifier.fromNamespaceAndPath(BlockBattlesMod.MOD_ID, "warps/" + structurePath);
  }
}
