package com.remy.blockbattles.game.blocks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CreateBlocks {
  private static BattleBlock block(
      BattleBlockIDs id,
      String name,
      String description,
      int damage,
      int defence,
      int healing,
      int defenceDamage) {
    return block(
        id,
        name,
        description,
        damage,
        false,
        defence,
        false,
        healing,
        false,
        defenceDamage,
        false);
  }

  private static BattleBlock block(
      BattleBlockIDs id,
      String name,
      String description,
      int damage,
      boolean damagePerTurn,
      int defence,
      boolean defencePerTurn,
      int healing,
      boolean healingPerTurn,
      int defenceDamage,
      boolean defenceDamagePerTurn) {
    BattleBlock battleBlock = new BattleBlock(
        id,
        name,
        description,
        Classification.NATURAL,
        damage,
        healing,
        defence,
        defenceDamage);

    battleBlock.damagePerTurn = damagePerTurn;
    battleBlock.defencePerTurn = defencePerTurn;
    battleBlock.healingPerTurn = healingPerTurn;
    battleBlock.defenceDamagePerTurn = defenceDamagePerTurn;

    return battleBlock;
  }

  public static final BattleBlock GRASS_BLOCK = block(
      BattleBlockIDs.GRASS_BLOCK, "Grass",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DIRT = block(
      BattleBlockIDs.DIRT, "Dirt",
      "Unchoosable.",
      0, 0, 0, 0);

  public static final BattleBlock MYCELIUM = block(
      BattleBlockIDs.MYCELIUM, "Mycelium",
      "Unchoosable. Damage happens per turn.",
      8, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock PODZOL = block(
      BattleBlockIDs.PODZOL, "Podzol",
      "Unchoosable. Defence happens per turn.",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock FARMLAND = block(
      BattleBlockIDs.FARMLAND, "Farmland",
      "Unchoosable. Healing happens per turn.",
      0, false,
      0, false,
      11, true,
      0, false);

  public static final BattleBlock SAND = block(
      BattleBlockIDs.SAND, "Sand",
      "None",
      0, 2, 0, 2);

  public static final BattleBlock SANDSTONE = block(
      BattleBlockIDs.SANDSTONE, "Sandstone",
      "None",
      6, 1, 6, 1);

  public static final BattleBlock SMOOTH_SANDSTONE = block(
      BattleBlockIDs.SMOOTH_SANDSTONE, "Smooth Sandstone",
      "None",
      10, 0, 10, 0);

  public static final BattleBlock CUT_SANDSTONE = block(
      BattleBlockIDs.CUT_SANDSTONE, "Cut Sandstone",
      "None",
      3, 0, 3, 3);

  public static final BattleBlock RED_SAND = block(
      BattleBlockIDs.RED_SAND, "Red Sand",
      "None",
      -10, 4, -8, 4);

  public static final BattleBlock RED_SANDSTONE = block(
      BattleBlockIDs.RED_SANDSTONE, "Red Sandstone",
      "None",
      -4, 3, -4, 3);

  public static final BattleBlock SMOOTH_RED_SANDSTONE = block(
      BattleBlockIDs.SMOOTH_RED_SANDSTONE, "Smooth Red Sandstone",
      "None",
      -8, 4, -9, 4);

  public static final BattleBlock CUT_RED_SANDSTONE = block(
      BattleBlockIDs.CUT_RED_SANDSTONE, "Cut Red Sandstone",
      "None",
      20, -2, 20, -2);

  public static final BattleBlock CHISELED_RED_SANDSTONE = block(
      BattleBlockIDs.CHISELED_RED_SANDSTONE, "Chiseled Red Sandstone",
      "None",
      40, -4, 40, -4);

  public static final BattleBlock SNOW = block(
      BattleBlockIDs.SNOW, "Snow",
      "Defence happens per turn. Minimum zero.",
      0, false,
      -1, true,
      0, false,
      0, false);

  public static final BattleBlock POWDERED_SNOW = block(
      BattleBlockIDs.POWDER_SNOW, "Powdered Snow",
      "None",
      12, 0, 0, 0);

  public static final BattleBlock MOSS_BLOCK = block(
      BattleBlockIDs.MOSS_BLOCK, "Moss Block",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DEAD_BUSH = block(
      BattleBlockIDs.DEAD_BUSH, "Dead Bush",
      "Damage happens per turn.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock CACTUS = block(
      BattleBlockIDs.CACTUS, "Cactus",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock RED_TULIP = block(
      BattleBlockIDs.RED_TULIP, "Red Tulip",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CORNFLOWER = block(
      BattleBlockIDs.CORNFLOWER, "Cornflower",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock PINK_PETALS = block(
      BattleBlockIDs.PINK_PETALS, "Pink Petals",
      "None",
      0, 0, 10, 0);

  public static final BattleBlock TORCHFLOWER = block(
      BattleBlockIDs.TORCHFLOWER, "Torchflower",
      "None",
      12, 0, 0, 0);

  public static final BattleBlock CARVED_PUMPKIN = block(
      BattleBlockIDs.CARVED_PUMPKIN, "Carved Pumpkin",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock WATER = block(
      BattleBlockIDs.WATER, "Water",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CHERRY_LEAVES = block(
      BattleBlockIDs.CHERRY_LEAVES, "Cherry Leaves",
      "Heals a tenth of your max health.",
      0, 0, 0, 0);

  public static final BattleBlock CHERRY_LOG = block(
      BattleBlockIDs.CHERRY_LOG, "Cherry Log",
      "Healing happens per turn. Every turn 1 Cherry Log gets placed above that Cherry Log if there isn't already a block above.",
      0, false,
      0, false,
      3, true,
      0, false);

  public static final BattleBlock JUNGLE_LOG = block(
      BattleBlockIDs.JUNGLE_LOG, "Jungle Log",
      "Defence happens per turn. Every turn 1 Jungle Log gets placed above that Jungle Log if there isn't already a block above.",
      0, false,
      1, true,
      0, false,
      0, false);

  public static final BattleBlock HORN_CORAL_BLOCK = block(
      BattleBlockIDs.HORN_CORAL_BLOCK, "Horn Coral Block",
      "None",
      20, 0, 0, 0);

  public static final BattleBlock TUBE_CORAL_BLOCK = block(
      BattleBlockIDs.TUBE_CORAL_BLOCK, "Tube Coral Block",
      "None",
      0, 4, 0, 0);

  public static final BattleBlock BUBBLE_CORAL_BLOCK = block(
      BattleBlockIDs.BUBBLE_CORAL_BLOCK, "Bubble Coral Block",
      "None",
      0, 0, 18, 0);

  public static final BattleBlock FIRE_CORAL_BLOCK = block(
      BattleBlockIDs.FIRE_CORAL_BLOCK, "Fire Coral Block",
      "None",
      0, 0, 0, 4);

  public static final BattleBlock BUBBLE_CORAL_FAN = block(
      BattleBlockIDs.BUBBLE_CORAL_FAN, "Bubble Coral Fan",
      "None",
      0, 0, 30, 0);

  public static final BattleBlock HORN_CORAL_FAN = block(
      BattleBlockIDs.HORN_CORAL_FAN, "Horn Coral Fan",
      "None",
      34, 0, 0, 0);

  public static final BattleBlock TUBE_CORAL_FAN = block(
      BattleBlockIDs.TUBE_CORAL_FAN, "Tube Coral Fan",
      "None",
      0, 7, 0, 0);

  public static final BattleBlock PRISMARINE = block(
      BattleBlockIDs.PRISMARINE, "Prismarine",
      "None",
      35, 0, 0, -4);

  public static final BattleBlock NETHERRACK = block(
      BattleBlockIDs.NETHERRACK, "Netherrack",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock NETHER_GOLD_ORE = block(
      BattleBlockIDs.NETHER_GOLD_ORE, "Nether Gold Ore",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock NETHER_QUARTZ_ORE = block(
      BattleBlockIDs.NETHER_QUARTZ_ORE, "Nether Quartz Ore",
      "None",
      16, 0, 0, 0);

  public static final BattleBlock CRIMSON_NYLIUM = block(
      BattleBlockIDs.CRIMSON_NYLIUM, "Crimson Nylium",
      "Unchoosable. Damage happens per turn.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock WARPED_NYLIUM = block(
      BattleBlockIDs.WARPED_NYLIUM, "Warped Nylium",
      "Unchoosable. Healing happens per turn.",
      0, false,
      0, false,
      6, true,
      0, false);

  public static final BattleBlock CRIMSON_HYPHAE = block(
      BattleBlockIDs.CRIMSON_HYPHAE, "Crimson Hyphae",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock WARPED_HYPHAE = block(
      BattleBlockIDs.WARPED_HYPHAE, "Warped Hyphae",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SOUL_SAND = block(
      BattleBlockIDs.SOUL_SAND, "Soul Sand",
      "None",
      12, 0, 2, 0);

  public static final BattleBlock GLOWSTONE = block(
      BattleBlockIDs.GLOWSTONE, "Glowstone",
      "None",
      0, 0, 12, 0);

  public static final BattleBlock CANDLE = block(
      BattleBlockIDs.CANDLE, "Candle",
      "None",
      0, 0, 16, 0);

  public static final BattleBlock MAGMA_BLOCK = block(
      BattleBlockIDs.MAGMA_BLOCK, "Magma Block",
      "Damage and healing happen per turn.",
      10, true,
      0, false,
      -2, true,
      0, false);

  public static final BattleBlock SOUL_TORCH = block(
      BattleBlockIDs.SOUL_TORCH, "Soul Torch",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SOUL_LANTERN = block(
      BattleBlockIDs.SOUL_LANTERN, "Soul Lantern",
      "None",
      20, 0, 0, 0);

  public static final BattleBlock CAMPFIRE = block(
      BattleBlockIDs.CAMPFIRE, "Campfire",
      "Damage happens per turn.",
      10, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock SOUL_CAMPFIRE = block(
      BattleBlockIDs.SOUL_CAMPFIRE, "Soul Campfire",
      "Damage happens per turn.",
      5, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock WITHER_ROSE = block(
      BattleBlockIDs.WITHER_ROSE, "Wither Rose",
      "None",
      6, 0, 0, 0);

  public static final BattleBlock NETHER_BRICKS = block(
      BattleBlockIDs.NETHER_BRICKS, "Nether Bricks",
      "Damage and defence happen per turn.",
      6, true,
      0, true,
      0, false,
      0, false);

  public static final BattleBlock RESPAWN_ANCHOR = block(
      BattleBlockIDs.RESPAWN_ANCHOR, "Respawn Anchor",
      "Damage happens per turn.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock ANCIENT_DEBRIS = block(
      BattleBlockIDs.ANCIENT_DEBRIS, "Ancient Debris",
      "None",
      0, 3, 3, 0);

  public static final BattleBlock NETHERITE_BLOCK = block(
      BattleBlockIDs.NETHERITE_BLOCK, "Block of Netherite",
      "None",
      0, 8, 0, 6);

  public static final BattleBlock END_STONE = block(
      BattleBlockIDs.END_STONE, "Endstone",
      "Defence happens per turn.",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock END_CRYSTAL = block(
      BattleBlockIDs.END_CRYSTAL, "End Crystal",
      "None",
      12, 0, 0, 0);

  public static final BattleBlock DRAGON_EGG = block(
      BattleBlockIDs.DRAGON_EGG, "Dragon Egg",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock FURNACE = block(
      BattleBlockIDs.FURNACE, "Furnace",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CHEST = block(
      BattleBlockIDs.CHEST, "Chest",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock TRAPPED_CHEST = block(
      BattleBlockIDs.TRAPPED_CHEST, "Trapped Chest",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CAULDRON = block(
      BattleBlockIDs.CAULDRON, "Cauldron",
      "Defence happens per turn.",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock COMPOSTER = block(
      BattleBlockIDs.COMPOSTER, "Composter",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock ANVIL = block(
      BattleBlockIDs.ANVIL, "Anvil",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DAMAGED_ANVIL = block(
      BattleBlockIDs.DAMAGED_ANVIL, "Damaged Anvil",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock STONECUTTER = block(
      BattleBlockIDs.STONECUTTER, "Stonecutter",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock LOOM = block(
      BattleBlockIDs.LOOM, "Loom",
      "Missing values treated as zero.",
      0, 0, 0, 0);

  public static final BattleBlock CARTOGRAPHY_TABLE = block(
      BattleBlockIDs.CARTOGRAPHY_TABLE, "Cartography Table",
      "Missing values treated as zero.",
      0, 0, 0, 0);

  public static final BattleBlock LECTERN = block(
      BattleBlockIDs.LECTERN, "Lectern",
      "None",
      6, 0, 0, 0);

  public static final BattleBlock BREWING_STAND = block(
      BattleBlockIDs.BREWING_STAND, "Brewing Stand",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SMITHING_TABLE = block(
      BattleBlockIDs.SMITHING_TABLE, "Smithing Table",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock ENCHANTING_TABLE = block(
      BattleBlockIDs.ENCHANTING_TABLE, "Enchanting Table",
      "None",
      0, 0, -35, 0);

  public static final BattleBlock REPEATER = block(
      BattleBlockIDs.REPEATER, "Repeater",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DAYLIGHT_SENSOR = block(
      BattleBlockIDs.DAYLIGHT_DETECTOR, "Daylight Sensor",
      "Damage happens per turn.",
      10, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock REDSTONE_TORCH = block(
      BattleBlockIDs.REDSTONE_TORCH, "Redstone Torch",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock TORCH = block(
      BattleBlockIDs.TORCH, "Torch",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock COPPER_TORCH = block(
      BattleBlockIDs.COPPER_TORCH, "Copper Torch",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock LIGHTNING_ROD = block(
      BattleBlockIDs.LIGHTNING_ROD, "Lightning Rod",
      "Healing happens per turn. Missing damage value treated as zero.",
      0, false,
      0, false,
      -4, true,
      0, false);

  public static final BattleBlock DISPENSER = block(
      BattleBlockIDs.DISPENSER, "Dispenser",
      "Damage happens per turn.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock MONSTER_SPAWNER = block(
      BattleBlockIDs.SPAWNER, "Monster Spawner",
      "Damage and healing happen per turn.",
      22, true,
      0, false,
      -10, true,
      0, false);

  public static final BattleBlock SCULK_SENSOR = block(
      BattleBlockIDs.SCULK_SENSOR, "Sculk Sensor",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CALIBRATED_SCULK_SENSOR = block(
      BattleBlockIDs.CALIBRATED_SCULK_SENSOR, "Calibrated Sculk Sensor",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SCULK_SHRIEKER = block(
      BattleBlockIDs.SCULK_SHRIEKER, "Sculk Shrieker",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SCULK_CATALYST = block(
      BattleBlockIDs.SCULK_CATALYST, "Sculk Catalyst",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SCULK = block(
      BattleBlockIDs.SCULK, "Sculk",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CHISELED_BOOKSHELF = block(
      BattleBlockIDs.CHISELED_BOOKSHELF, "Chiseled Bookshelf",
      "None",
      0, 0, 4, 0);

  public static final BattleBlock BOOKSHELF = block(
      BattleBlockIDs.BOOKSHELF, "Bookshelf",
      "None",
      0, 0, 12, 0);

  public static final BattleBlock SHULKER_BOX = block(
      BattleBlockIDs.SHULKER_BOX, "Shulker Box",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock VAULT = block(
      BattleBlockIDs.VAULT, "Vault",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CONDUIT = block(
      BattleBlockIDs.CONDUIT, "Conduit",
      "Healing happens per turn.",
      0, false,
      0, false,
      7, true,
      0, false);

  public static final BattleBlock BEACON = block(
      BattleBlockIDs.BEACON, "Beacon",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE = block(
      BattleBlockIDs.DEEPSLATE, "Deepslate",
      "None",
      0, 5, 0, 0);

  public static final BattleBlock REINFORCED_DEEPSLATE = block(
      BattleBlockIDs.REINFORCED_DEEPSLATE, "Reinforced Deepslate",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_BRICKS = block(
      BattleBlockIDs.DEEPSLATE_BRICKS, "Deepslate Bricks",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_TILES = block(
      BattleBlockIDs.DEEPSLATE_TILES, "Deepslate Tiles",
      "None",
      0, 9, -20, 0);

  public static final BattleBlock DEEPSLATE_GOLD_ORE = block(
      BattleBlockIDs.DEEPSLATE_GOLD_ORE, "Deepslate Gold Ore",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_REDSTONE_ORE = block(
      BattleBlockIDs.DEEPSLATE_REDSTONE_ORE, "Deepslate Redstone Ore",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SMOOTH_STONE = block(
      BattleBlockIDs.SMOOTH_STONE, "Smooth Stone",
      "Defence and defence damage happen per turn.",
      0, false,
      -1, true,
      0, false,
      2, true);

  public static final BattleBlock STONE_BRICKS = block(
      BattleBlockIDs.STONE_BRICKS, "Stone Bricks",
      "None",
      0, 7, 0, 0);

  public static final BattleBlock CRACKED_STONE_BRICKS = block(
      BattleBlockIDs.CRACKED_STONE_BRICKS, "Cracked Stone Bricks",
      "None",
      0, 0, 0, -6);

  public static final BattleBlock COBBLESTONE = block(
      BattleBlockIDs.COBBLESTONE, "Cobblestone",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock POLISHED_BLACKSTONE_BRICKS = block(
      BattleBlockIDs.POLISHED_BLACKSTONE_BRICKS, "Polished Blackstone Bricks",
      "None",
      12, 0, 0, 0);

  public static final BattleBlock GLASS = block(
      BattleBlockIDs.GLASS, "Glass",
      "N/A stats treated as zero.",
      0, 0, 0, 0);

  public static final BattleBlock GLASS_PANE = block(
      BattleBlockIDs.GLASS_PANE, "Glass Pane",
      "Damage, defence, and healing happen per turn.",
      0, true,
      0, true,
      0, true,
      0, false);

  public static final BattleBlock COAL_BLOCK = block(
      BattleBlockIDs.COAL_BLOCK, "Block of Coal",
      "None",
      0, 3, 0, 0);

  public static final BattleBlock IRON_BLOCK = block(
      BattleBlockIDs.IRON_BLOCK, "Block of Iron",
      "None",
      0, 5, 0, 0);

  public static final BattleBlock GOLD_BLOCK = block(
      BattleBlockIDs.GOLD_BLOCK, "Block of Gold",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock EMERALD_BLOCK = block(
      BattleBlockIDs.EMERALD_BLOCK, "Block of Emerald",
      "None",
      0, -3, -10, 0);

  public static final BattleBlock LAPIS_BLOCK = block(
      BattleBlockIDs.LAPIS_BLOCK, "Block of Lapis Lazuli",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock REDSTONE_BLOCK = block(
      BattleBlockIDs.REDSTONE_BLOCK, "Block of Redstone",
      "None",
      -20, -5, -20, 0);

  public static final BattleBlock DIAMOND_BLOCK = block(
      BattleBlockIDs.DIAMOND_BLOCK, "Block of Diamond",
      "None",
      24, 4, 24, 0);

  public static final BattleBlock COPPER_BLOCK = block(
      BattleBlockIDs.COPPER_BLOCK, "Copper Block",
      "Defence happens per turn.",
      0, false,
      0, true,
      0, false,
      0, false);

  public static final BattleBlock CHISELED_COPPER = block(
      BattleBlockIDs.CHISELED_COPPER, "Chiseled Copper",
      "Missing defence value treated as zero.",
      0, 0, 0, 0);

  public static final BattleBlock COPPER_GRATE = block(
      BattleBlockIDs.COPPER_GRATE, "Copper Grate",
      "Damage happens per turn.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock COPPER_BULB = block(
      BattleBlockIDs.COPPER_BULB, "Copper Bulb",
      "Healing happens per turn.",
      0, false,
      0, false,
      5, true,
      0, false);

  public static final BattleBlock COPPER_LANTERN = block(
      BattleBlockIDs.COPPER_LANTERN, "Copper Lantern",
      "Missing values treated as zero.",
      0, 0, 0, 0);

  public static final BattleBlock OBSIDIAN = block(
      BattleBlockIDs.OBSIDIAN, "Obsidian",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CRYING_OBSIDIAN = block(
      BattleBlockIDs.CRYING_OBSIDIAN, "Crying Obsidian",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock BEDROCK = block(
      BattleBlockIDs.BEDROCK, "Bedrock",
      "None",
      0, 12, 0, 0);

  public static final BattleBlock LAVA = block(
      BattleBlockIDs.LAVA, "Lava",
      "Damage happens per turn.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock TNT = block(
      BattleBlockIDs.TNT, "TNT",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock POINTED_DRIPSTONE = block(
      BattleBlockIDs.POINTED_DRIPSTONE, "Pointed Dripstone",
      "None",
      6, 0, 0, 0);

  public static final BattleBlock RED_CARPET = block(
      BattleBlockIDs.RED_CARPET, "Red Carpet",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock BLUE_CARPET = block(
      BattleBlockIDs.BLUE_CARPET, "Blue Carpet",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock GREEN_CARPET = block(
      BattleBlockIDs.GREEN_CARPET, "Green Carpet",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock RED_BED = block(
      BattleBlockIDs.RED_BED, "Red Bed",
      "None",
      0, 0, 8, 0);

  public static final BattleBlock CAKE = block(
      BattleBlockIDs.CAKE, "Cake",
      "Healing happens per turn.",
      -10, false,
      0, false,
      12, true,
      0, false);

  public static final BattleBlock MUSHROOM_STEM = block(
      BattleBlockIDs.MUSHROOM_STEM, "Mushroom Stem",
      "Damage happens per turn.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock COCOA = block(
      BattleBlockIDs.COCOA, "Cocoa Beans",
      "Healing happens per turn.",
      0, false,
      0, false,
      9, true,
      0, false);

  public static final BattleBlock SLIME_BLOCK = block(
      BattleBlockIDs.SLIME_BLOCK, "Slime Block",
      "Damage happens per turn.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock PLAYER_HEAD = block(
      BattleBlockIDs.PLAYER_HEAD, "Player Head",
      "None",
      -8, -2, -10, 0);

  public static final BattleBlock CREEPER_HEAD = block(
      BattleBlockIDs.CREEPER_HEAD, "Creeper Head",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock PIGLIN_HEAD = block(
      BattleBlockIDs.PIGLIN_HEAD, "Piglin Head",
      "None",
      10, 0, 0, 0);

  public static final BattleBlock SKELETON_SKULL = block(
      BattleBlockIDs.SKELETON_SKULL, "Skeleton Skull",
      "Damage happens per turn.",
      18, true,
      0, false,
      -16, false,
      0, false);

  public static final BattleBlock WITHER_SKELETON_SKULL = block(
      BattleBlockIDs.WITHER_SKELETON_SKULL, "Wither Skeleton Skull",
      "None",
      18, 0, 0, 0);

  public static final BattleBlock ZOMBIE_HEAD = block(
      BattleBlockIDs.ZOMBIE_HEAD, "Zombie Head",
      "Damage happens per turn.",
      14, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock DRAGON_HEAD = block(
      BattleBlockIDs.DRAGON_HEAD, "Dragon Head",
      "None",
      46, 0, 0, 0);

  public static final BattleBlock OAK_PLANKS = block(
      BattleBlockIDs.OAK_PLANKS, "Oak Planks",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock RAW_IRON_BLOCK = block(
      BattleBlockIDs.RAW_IRON_BLOCK, "Block of Raw Iron",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock RAW_GOLD_BLOCK = block(
      BattleBlockIDs.RAW_GOLD_BLOCK, "Block of Raw Gold",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock RAW_COPPER_BLOCK = block(
      BattleBlockIDs.RAW_COPPER_BLOCK, "Block of Raw Copper",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock PALE_OAK_LOG = block(
      BattleBlockIDs.PALE_OAK_LOG, "Pale Oak Log",
      "Damage and healing happen per turn.",
      10, true,
      0, false,
      -5, true,
      0, false);

  public static final BattleBlock PALE_MOSS_BLOCK = block(
      BattleBlockIDs.PALE_MOSS_BLOCK, "Pale Moss Block",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock PALE_MOSS_CARPET = block(
      BattleBlockIDs.PALE_MOSS_CARPET, "Pale Moss Carpet",
      "Unchoosable.",
      0, 0, 0, 0);

  public static final BattleBlock CREAKING_HEART = block(
      BattleBlockIDs.CREAKING_HEART, "Creaking Heart",
      "None",
      35, 0, -15, 0);

  // The spreadsheet row named "???" is omitted because it does not identify a
  // block ID.

  public static final List<BattleBlock> ALL = List.of(
      GRASS_BLOCK,
      DIRT,
      MYCELIUM,
      PODZOL,
      FARMLAND,
      SAND,
      SANDSTONE,
      SMOOTH_SANDSTONE,
      CUT_SANDSTONE,
      RED_SAND,
      RED_SANDSTONE,
      SMOOTH_RED_SANDSTONE,
      CUT_RED_SANDSTONE,
      CHISELED_RED_SANDSTONE,
      SNOW,
      POWDERED_SNOW,
      MOSS_BLOCK,
      DEAD_BUSH,
      CACTUS,
      RED_TULIP,
      CORNFLOWER,
      PINK_PETALS,
      TORCHFLOWER,
      CARVED_PUMPKIN,
      WATER,
      CHERRY_LEAVES,
      CHERRY_LOG,
      JUNGLE_LOG,
      HORN_CORAL_BLOCK,
      TUBE_CORAL_BLOCK,
      BUBBLE_CORAL_BLOCK,
      FIRE_CORAL_BLOCK,
      BUBBLE_CORAL_FAN,
      HORN_CORAL_FAN,
      TUBE_CORAL_FAN,
      PRISMARINE,
      NETHERRACK,
      NETHER_GOLD_ORE,
      NETHER_QUARTZ_ORE,
      CRIMSON_NYLIUM,
      WARPED_NYLIUM,
      CRIMSON_HYPHAE,
      WARPED_HYPHAE,
      SOUL_SAND,
      GLOWSTONE,
      CANDLE,
      MAGMA_BLOCK,
      SOUL_TORCH,
      SOUL_LANTERN,
      CAMPFIRE,
      SOUL_CAMPFIRE,
      WITHER_ROSE,
      NETHER_BRICKS,
      RESPAWN_ANCHOR,
      ANCIENT_DEBRIS,
      NETHERITE_BLOCK,
      END_STONE,
      END_CRYSTAL,
      DRAGON_EGG,
      FURNACE,
      CHEST,
      TRAPPED_CHEST,
      CAULDRON,
      COMPOSTER,
      ANVIL,
      DAMAGED_ANVIL,
      STONECUTTER,
      LOOM,
      CARTOGRAPHY_TABLE,
      LECTERN,
      BREWING_STAND,
      SMITHING_TABLE,
      ENCHANTING_TABLE,
      REPEATER,
      DAYLIGHT_SENSOR,
      REDSTONE_TORCH,
      TORCH,
      COPPER_TORCH,
      LIGHTNING_ROD,
      DISPENSER,
      MONSTER_SPAWNER,
      SCULK_SENSOR,
      CALIBRATED_SCULK_SENSOR,
      SCULK_SHRIEKER,
      SCULK_CATALYST,
      SCULK,
      CHISELED_BOOKSHELF,
      BOOKSHELF,
      SHULKER_BOX,
      VAULT,
      CONDUIT,
      BEACON,
      DEEPSLATE,
      REINFORCED_DEEPSLATE,
      DEEPSLATE_BRICKS,
      DEEPSLATE_TILES,
      DEEPSLATE_GOLD_ORE,
      DEEPSLATE_REDSTONE_ORE,
      SMOOTH_STONE,
      STONE_BRICKS,
      CRACKED_STONE_BRICKS,
      COBBLESTONE,
      POLISHED_BLACKSTONE_BRICKS,
      GLASS,
      GLASS_PANE,
      COAL_BLOCK,
      IRON_BLOCK,
      GOLD_BLOCK,
      EMERALD_BLOCK,
      LAPIS_BLOCK,
      REDSTONE_BLOCK,
      DIAMOND_BLOCK,
      COPPER_BLOCK,
      CHISELED_COPPER,
      COPPER_GRATE,
      COPPER_BULB,
      COPPER_LANTERN,
      OBSIDIAN,
      CRYING_OBSIDIAN,
      BEDROCK,
      LAVA,
      TNT,
      POINTED_DRIPSTONE,
      RED_CARPET,
      BLUE_CARPET,
      GREEN_CARPET,
      RED_BED,
      CAKE,
      MUSHROOM_STEM,
      COCOA,
      SLIME_BLOCK,
      PLAYER_HEAD,
      CREEPER_HEAD,
      PIGLIN_HEAD,
      SKELETON_SKULL,
      WITHER_SKELETON_SKULL,
      ZOMBIE_HEAD,
      DRAGON_HEAD,
      OAK_PLANKS,
      RAW_IRON_BLOCK,
      RAW_GOLD_BLOCK,
      RAW_COPPER_BLOCK,
      PALE_OAK_LOG,
      PALE_MOSS_BLOCK,
      PALE_MOSS_CARPET,
      CREAKING_HEART);

  private static final Map<String, BattleBlock> BY_MINECRAFT_ID = ALL.stream()
      .collect(Collectors.toUnmodifiableMap(block -> block.id.getId(), Function.identity()));

  public static Optional<BattleBlock> findByMinecraftId(String blockId) {
    return Optional.ofNullable(BY_MINECRAFT_ID.get(blockId));
  }
}
