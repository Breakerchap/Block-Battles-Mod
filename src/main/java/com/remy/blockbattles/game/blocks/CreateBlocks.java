package com.remy.blockbattles.game.blocks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CreateBlocks {
  private static final String GRASS_VARIANTS_REQUIREMENT =
      "Place on Grass, Dirt, Podzol, Mycelium, or Farmland.";
  private static final String CACTUS_REQUIREMENT =
      "Place on Sand, Red Sand, Grass, Dirt, Podzol, Mycelium, or Farmland. No adjacent blocks.";
  private static final String CORAL_BLOCK_REQUIREMENT =
      "Place on Horn Coral Block, Tube Coral Block, Bubble Coral Block, or Fire Coral Block.";
  private static final String FLOWERING_LOG_REQUIREMENT =
      "Place on Cherry Log, Jungle Log, Pale Oak Log, or Cherry Leaves.";
  private static final String COCOA_LOG_REQUIREMENT =
      "Place on Cherry Log, Jungle Log, or Pale Oak Log.";

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
        classificationFor(id),
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

  private static BattleBlock requireOn(BattleBlock battleBlock, String requirementDescription, BattleBlockIDs... requiredSupportBlocks) {
    return battleBlock.withPlacementRequirements(requirementDescription, requiredSupportBlocks);
  }

  private static BattleBlock requireOnGrassVariants(BattleBlock battleBlock) {
    return requireOn(
        battleBlock,
        GRASS_VARIANTS_REQUIREMENT,
        BattleBlockIDs.GRASS_BLOCK,
        BattleBlockIDs.DIRT,
        BattleBlockIDs.PODZOL,
        BattleBlockIDs.MYCELIUM,
        BattleBlockIDs.FARMLAND);
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
      "Unchoosable.",
      8, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock PODZOL = block(
      BattleBlockIDs.PODZOL, "Podzol",
      "Unchoosable.",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock FARMLAND = block(
      BattleBlockIDs.FARMLAND, "Farmland",
      "Unchoosable.",
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
      "Your opponent draws 1 less block while this is in play.",
      0, false,
      -1, true,
      0, false,
      0, false);

  public static final BattleBlock POWDERED_SNOW = block(
      BattleBlockIDs.POWDER_SNOW, "Powdered Snow",
      "Deals 4 damage next turn if this is still in play.",
      12, 0, 0, 0);

  public static final BattleBlock MOSS_BLOCK = block(
      BattleBlockIDs.MOSS_BLOCK, "Moss Block",
      "Replaces all blocks within 1 block of it with grass.",
      0, 0, 0, 0);

  public static final BattleBlock DEAD_BUSH = block(
      BattleBlockIDs.DEAD_BUSH, "Dead Bush",
      "Deals 1 extra damage for every 5 health its owner is missing.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock CACTUS = requireOn(
      block(
          BattleBlockIDs.CACTUS, "Cactus",
          "Reflects half of all health damage you take back to the attacker, rounded down.",
          0, 0, 0, 0),
      CACTUS_REQUIREMENT,
      BattleBlockIDs.SAND,
      BattleBlockIDs.RED_SAND,
      BattleBlockIDs.GRASS_BLOCK,
      BattleBlockIDs.DIRT,
      BattleBlockIDs.PODZOL,
      BattleBlockIDs.MYCELIUM,
      BattleBlockIDs.FARMLAND);

  public static final BattleBlock RED_TULIP = requireOnGrassVariants(
      block(
          BattleBlockIDs.RED_TULIP, "Red Tulip",
          "Increase your team's max health by 20.",
          0, 0, 0, 0));

  public static final BattleBlock CORNFLOWER = requireOnGrassVariants(
      block(
          BattleBlockIDs.CORNFLOWER, "Cornflower",
          "Heals a quarter of your current health.",
          0, 0, 0, 0));

  public static final BattleBlock PINK_PETALS = requireOnGrassVariants(
      block(
          BattleBlockIDs.PINK_PETALS, "Pink Petals",
          "All healing on your next turn increases your max health as well.",
          0, 0, 10, 0));

  public static final BattleBlock TORCHFLOWER = requireOnGrassVariants(
      block(
          BattleBlockIDs.TORCHFLOWER, "Torchflower",
          "None",
          12, 0, 0, 0));

  public static final BattleBlock CARVED_PUMPKIN = block(
      BattleBlockIDs.CARVED_PUMPKIN, "Carved Pumpkin",
      "Gives 4 healing for each block within 1 block.",
      0, 0, 0, 0);

  public static final BattleBlock WATER = block(
      BattleBlockIDs.WATER, "Water",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CHERRY_LEAVES = requireOn(
      block(
          BattleBlockIDs.CHERRY_LEAVES, "Cherry Leaves",
          "Heal 10% of your max health.",
          0, 0, 0, 0),
      FLOWERING_LOG_REQUIREMENT,
      BattleBlockIDs.CHERRY_LOG,
      BattleBlockIDs.JUNGLE_LOG,
      BattleBlockIDs.PALE_OAK_LOG,
      BattleBlockIDs.CHERRY_LEAVES);

  public static final BattleBlock CHERRY_LOG = requireOnGrassVariants(
      block(
          BattleBlockIDs.CHERRY_LOG, "Cherry Log",
          "Every turn, 1 Cherry Log gets placed above it if the block above is air.",
          0, false,
          0, false,
          3, true,
          0, false));

  public static final BattleBlock JUNGLE_LOG = requireOnGrassVariants(
      block(
          BattleBlockIDs.JUNGLE_LOG, "Jungle Log",
          "Every turn, 1 Jungle Log gets placed above it if the block above is air.",
          0, false,
          1, true,
          0, false,
          0, false));

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

  public static final BattleBlock BUBBLE_CORAL_FAN = requireOn(
      block(
          BattleBlockIDs.BUBBLE_CORAL_FAN, "Bubble Coral Fan",
          "None",
          0, 0, 30, 0),
      CORAL_BLOCK_REQUIREMENT,
      BattleBlockIDs.HORN_CORAL_BLOCK,
      BattleBlockIDs.TUBE_CORAL_BLOCK,
      BattleBlockIDs.BUBBLE_CORAL_BLOCK,
      BattleBlockIDs.FIRE_CORAL_BLOCK);

  public static final BattleBlock HORN_CORAL_FAN = requireOn(
      block(
          BattleBlockIDs.HORN_CORAL_FAN, "Horn Coral Fan",
          "None",
          34, 0, 0, 0),
      CORAL_BLOCK_REQUIREMENT,
      BattleBlockIDs.HORN_CORAL_BLOCK,
      BattleBlockIDs.TUBE_CORAL_BLOCK,
      BattleBlockIDs.BUBBLE_CORAL_BLOCK,
      BattleBlockIDs.FIRE_CORAL_BLOCK);

  public static final BattleBlock TUBE_CORAL_FAN = requireOn(
      block(
          BattleBlockIDs.TUBE_CORAL_FAN, "Tube Coral Fan",
          "None",
          0, 7, 0, 0),
      CORAL_BLOCK_REQUIREMENT,
      BattleBlockIDs.HORN_CORAL_BLOCK,
      BattleBlockIDs.TUBE_CORAL_BLOCK,
      BattleBlockIDs.BUBBLE_CORAL_BLOCK,
      BattleBlockIDs.FIRE_CORAL_BLOCK);

  public static final BattleBlock PRISMARINE = block(
      BattleBlockIDs.PRISMARINE, "Prismarine",
      "None",
      35, 0, 0, -4);

  public static final BattleBlock NETHERRACK = block(
      BattleBlockIDs.NETHERRACK, "Netherrack",
      "Deal the last amount of damage you dealt.",
      0, 0, 0, 0);

  public static final BattleBlock NETHER_GOLD_ORE = block(
      BattleBlockIDs.NETHER_GOLD_ORE, "Nether Gold Ore",
      "Each time you take damage, gain 1 defence. This does not affect the activating damage.",
      0, 0, 0, 0);

  public static final BattleBlock NETHER_QUARTZ_ORE = block(
      BattleBlockIDs.NETHER_QUARTZ_ORE, "Nether Quartz Ore",
      "If broken, deal 7 damage to your opponent.",
      16, 0, 0, 0);

  public static final BattleBlock CRIMSON_NYLIUM = block(
      BattleBlockIDs.CRIMSON_NYLIUM, "Crimson Nylium",
      "Unchoosable.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock WARPED_NYLIUM = block(
      BattleBlockIDs.WARPED_NYLIUM, "Warped Nylium",
      "Unchoosable.",
      0, false,
      0, false,
      6, true,
      0, false);

  public static final BattleBlock CRIMSON_HYPHAE = block(
      BattleBlockIDs.CRIMSON_HYPHAE, "Crimson Hyphae",
      "Deals 1 damage for each Otherworldly block on the board.",
      0, 0, 0, 0);

  public static final BattleBlock WARPED_HYPHAE = block(
      BattleBlockIDs.WARPED_HYPHAE, "Warped Hyphae",
      "Heals 1 for each Otherworldly block on the board.",
      0, 0, 0, 0);

  public static final BattleBlock SOUL_SAND = block(
      BattleBlockIDs.SOUL_SAND, "Soul Sand",
      "Your opponent draws 1 less block on their next turn.",
      12, 0, 2, 0);

  public static final BattleBlock GLOWSTONE = block(
      BattleBlockIDs.GLOWSTONE, "Glowstone",
      "None",
      0, 0, 12, 0);

  public static final BattleBlock CANDLE = requireOn(
      block(
          BattleBlockIDs.CANDLE, "Candle",
          "None",
          0, 0, 16, 0),
      "Place on Cake.",
      BattleBlockIDs.CAKE);

  public static final BattleBlock MAGMA_BLOCK = block(
      BattleBlockIDs.MAGMA_BLOCK, "Magma Block",
      "None",
      10, true,
      0, false,
      -2, true,
      0, false);

  public static final BattleBlock SOUL_TORCH = block(
      BattleBlockIDs.SOUL_TORCH, "Soul Torch",
      "Gives +4 healing from all sources on your next turn.",
      0, 0, 0, 0);

  public static final BattleBlock SOUL_LANTERN = block(
      BattleBlockIDs.SOUL_LANTERN, "Soul Lantern",
      "This damage bypasses defence.",
      20, 0, 0, 0);

  public static final BattleBlock CAMPFIRE = block(
      BattleBlockIDs.CAMPFIRE, "Campfire",
      "Leaves after 3 turns.",
      10, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock SOUL_CAMPFIRE = block(
      BattleBlockIDs.SOUL_CAMPFIRE, "Soul Campfire",
      "Damage from this only applies to health, ignoring defence.",
      5, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock WITHER_ROSE = requireOnGrassVariants(
      block(
          BattleBlockIDs.WITHER_ROSE, "Wither Rose",
          "None",
          6, 0, 0, 0));

  public static final BattleBlock NETHER_BRICKS = block(
      BattleBlockIDs.NETHER_BRICKS, "Nether Bricks",
      "None",
      6, true,
      0, true,
      0, false,
      0, false);

  public static final BattleBlock RESPAWN_ANCHOR = block(
      BattleBlockIDs.RESPAWN_ANCHOR, "Respawn Anchor",
      "Deals 2 damage per turn. With 2 adjacent Glowstone, revive at half health in Nether warps; otherwise it explodes for 5 damage to both teams.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock ANCIENT_DEBRIS = block(
      BattleBlockIDs.ANCIENT_DEBRIS, "Ancient Debris",
      "Unbreakable. If broken, deal 50 damage to your opponent.",
      0, 3, 3, 0);

  public static final BattleBlock NETHERITE_BLOCK = block(
      BattleBlockIDs.NETHERITE_BLOCK, "Block of Netherite",
      "None",
      0, 8, 0, 6);

  public static final BattleBlock END_STONE = block(
      BattleBlockIDs.END_STONE, "Endstone",
      "None",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock END_CRYSTAL = requireOn(
      block(
          BattleBlockIDs.END_CRYSTAL, "End Crystal",
          "Breaks all blocks within 2 blocks.",
          12, 0, 0, 0),
      "Place on Obsidian, Bedrock, or Block of Netherite.",
      BattleBlockIDs.OBSIDIAN,
      BattleBlockIDs.BEDROCK,
      BattleBlockIDs.NETHERITE_BLOCK);

  public static final BattleBlock DRAGON_EGG = block(
      BattleBlockIDs.DRAGON_EGG, "Dragon Egg",
      "After 2 turns, heal 32 health and the egg breaks.",
      0, 0, 0, 0);

  public static final BattleBlock FURNACE = block(
      BattleBlockIDs.FURNACE, "Furnace",
      "Gives +1 defence for each block within 1 block. Adjacent Block of Coal gives +5 defence each turn, and adjacent Campfire gives +6 healing each turn.",
      0, 0, 0, 0);

  public static final BattleBlock CHEST = block(
      BattleBlockIDs.CHEST, "Chest",
      "Breaks adjacent Man-made blocks. Enemy ones are destroyed, and your destroyed ones activate for you.",
      0, 0, 0, 0);

  public static final BattleBlock TRAPPED_CHEST = block(
      BattleBlockIDs.TRAPPED_CHEST, "Trapped Chest",
      "Breaks adjacent Man-made blocks. Enemy ones are destroyed, and your friendly ones are stored inside it and suppressed.",
      0, 0, 0, 0);

  public static final BattleBlock CAULDRON = block(
      BattleBlockIDs.CAULDRON, "Cauldron",
      "Gives 2 defence per turn. Water above heals 4 each turn. Lava above deals double damage, and Powdered Snow above deals double delayed damage.",
      0, false,
      2, true,
      0, false,
      0, false);

  public static final BattleBlock COMPOSTER = block(
      BattleBlockIDs.COMPOSTER, "Composter",
      "Absorbs adjacent Natural blocks, stores them, and activates their effects for you.",
      0, 0, 0, 0);

  public static final BattleBlock ANVIL = block(
      BattleBlockIDs.ANVIL, "Anvil",
      "Deals 6 damage for each block fallen after being placed.",
      0, 0, 0, 0);

  public static final BattleBlock DAMAGED_ANVIL = block(
      BattleBlockIDs.DAMAGED_ANVIL, "Damaged Anvil",
      "Deals 10 damage to your opponent and 5 damage to you for each block fallen after being placed.",
      0, 0, 0, 0);

  public static final BattleBlock STONECUTTER = block(
      BattleBlockIDs.STONECUTTER, "Stonecutter",
      "Absorbs adjacent Cave blocks, stores them, and activates their effects for you.",
      0, 0, 0, 0);

  public static final BattleBlock LOOM = block(
      BattleBlockIDs.LOOM, "Loom",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock CARTOGRAPHY_TABLE = block(
      BattleBlockIDs.CARTOGRAPHY_TABLE, "Cartography Table",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock LECTERN = block(
      BattleBlockIDs.LECTERN, "Lectern",
      "Deals +8 damage for each block in your hand.",
      6, 0, 0, 0);

  public static final BattleBlock BREWING_STAND = block(
      BattleBlockIDs.BREWING_STAND, "Brewing Stand",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SMITHING_TABLE = block(
      BattleBlockIDs.SMITHING_TABLE, "Smithing Table",
      "Deals 1 damage for each Man-made block on the board.",
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
      "None",
      10, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock REDSTONE_TORCH = block(
      BattleBlockIDs.REDSTONE_TORCH, "Redstone Torch",
      "Gives +5 damage from all sources on your next turn.",
      0, 0, 0, 0);

  public static final BattleBlock TORCH = block(
      BattleBlockIDs.TORCH, "Torch",
      "Gives +2 defence from all sources on your next turn.",
      0, 0, 0, 0);

  public static final BattleBlock COPPER_TORCH = block(
      BattleBlockIDs.COPPER_TORCH, "Copper Torch",
      "You take no damage on the next opponent turn.",
      0, 0, 0, 0);

  public static final BattleBlock LIGHTNING_ROD = block(
      BattleBlockIDs.LIGHTNING_ROD, "Lightning Rod",
      "None",
      0, false,
      0, false,
      -4, true,
      0, false);

  public static final BattleBlock DISPENSER = block(
      BattleBlockIDs.DISPENSER, "Dispenser",
      "None",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock MONSTER_SPAWNER = block(
      BattleBlockIDs.SPAWNER, "Monster Spawner",
      "None",
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
      "Lower your opponent's max health by 2.",
      0, 0, 0, 0);

  public static final BattleBlock CHISELED_BOOKSHELF = block(
      BattleBlockIDs.CHISELED_BOOKSHELF, "Chiseled Bookshelf",
      "Choose 2 battle blocks and add them to your next hand.",
      0, 0, 4, 0);

  public static final BattleBlock BOOKSHELF = block(
      BattleBlockIDs.BOOKSHELF, "Bookshelf",
      "Draw your entire deck as your hand next turn.",
      0, 0, 12, 0);

  public static final BattleBlock SHULKER_BOX = block(
      BattleBlockIDs.SHULKER_BOX, "Shulker Box",
      "Draw another block each turn.",
      0, 0, 0, 0);

  public static final BattleBlock VAULT = block(
      BattleBlockIDs.VAULT, "Vault",
      "Choose 1 card to permanently remove from your deck. If you do, gain 8 defence and 8 healing.",
      0, 0, 0, 0);

  public static final BattleBlock CONDUIT = block(
      BattleBlockIDs.CONDUIT, "Conduit",
      "Gives 7 defence per turn. With adjacent Water and Prismarine, revive at half health once.",
      0, false,
      0, false,
      7, true,
      0, false);

  public static final BattleBlock BEACON = block(
      BattleBlockIDs.BEACON, "Beacon",
      "On Block of Gold, gain 4 defence. On Block of Emerald, draw 2 extra cards each turn. On Block of Iron, take 1 less damage. On Block of Netherite, nearby blocks cannot be broken.",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE = block(
      BattleBlockIDs.DEEPSLATE, "Deepslate",
      "None",
      0, 5, 0, 0);

  public static final BattleBlock REINFORCED_DEEPSLATE = block(
      BattleBlockIDs.REINFORCED_DEEPSLATE, "Reinforced Deepslate",
      "Doubles your defence, with a max increase of 10.",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_BRICKS = block(
      BattleBlockIDs.DEEPSLATE_BRICKS, "Deepslate Bricks",
      "Deals double your current defence in damage.",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_TILES = block(
      BattleBlockIDs.DEEPSLATE_TILES, "Deepslate Tiles",
      "None",
      0, 9, -20, 0);

  public static final BattleBlock DEEPSLATE_GOLD_ORE = block(
      BattleBlockIDs.DEEPSLATE_GOLD_ORE, "Deepslate Gold Ore",
      "Steals half of your opponent's defence, rounded down.",
      0, 0, 0, 0);

  public static final BattleBlock DEEPSLATE_REDSTONE_ORE = block(
      BattleBlockIDs.DEEPSLATE_REDSTONE_ORE, "Deepslate Redstone Ore",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock SMOOTH_STONE = block(
      BattleBlockIDs.SMOOTH_STONE, "Smooth Stone",
      "None",
      0, false,
      -1, true,
      0, false,
      2, true);

  public static final BattleBlock STONE_BRICKS = block(
      BattleBlockIDs.STONE_BRICKS, "Stone Bricks",
      "Set your defence to 7.",
      0, 7, 0, 0);

  public static final BattleBlock CRACKED_STONE_BRICKS = block(
      BattleBlockIDs.CRACKED_STONE_BRICKS, "Cracked Stone Bricks",
      "Set your opponent's defence to 0.",
      0, 0, 0, -6);

  public static final BattleBlock COBBLESTONE = block(
      BattleBlockIDs.COBBLESTONE, "Cobblestone",
      "Removes all of your opponent's defence for 2 turns.",
      0, 0, 0, 0);

  public static final BattleBlock POLISHED_BLACKSTONE_BRICKS = block(
      BattleBlockIDs.POLISHED_BLACKSTONE_BRICKS, "Polished Blackstone Bricks",
      "Deals +3 damage for each adjacent block.",
      12, 0, 0, 0);

  public static final BattleBlock GLASS = block(
      BattleBlockIDs.GLASS, "Glass",
      "Copies and places the last placed block.",
      0, 0, 0, 0);

  public static final BattleBlock GLASS_PANE = block(
      BattleBlockIDs.GLASS_PANE, "Glass Pane",
      "Copies and places your opponent's last placed block.",
      0, true,
      0, true,
      0, true,
      0, false);

  public static final BattleBlock COAL_BLOCK = block(
      BattleBlockIDs.COAL_BLOCK, "Block of Coal",
      "If broken, break all blocks within 2 blocks of it and deal 10 damage to your opponent.",
      0, 3, 0, 0);

  public static final BattleBlock IRON_BLOCK = block(
      BattleBlockIDs.IRON_BLOCK, "Block of Iron",
      "None",
      0, 5, 0, 0);

  public static final BattleBlock GOLD_BLOCK = block(
      BattleBlockIDs.GOLD_BLOCK, "Block of Gold",
      "Triples your current defence and converts it into max health and current health. It is then replaced by Dirt in your deck and on the board.",
      0, 0, 0, 0);

  public static final BattleBlock EMERALD_BLOCK = block(
      BattleBlockIDs.EMERALD_BLOCK, "Block of Emerald",
      "Next turn, draw 2 more blocks and place 2 blocks total. It is then replaced by Dirt in your deck and on the board.",
      0, -3, -10, 0);

  public static final BattleBlock LAPIS_BLOCK = block(
      BattleBlockIDs.LAPIS_BLOCK, "Block of Lapis Lazuli",
      "Heals half of your current health, rounded up. It is then replaced by Dirt in your deck and on the board.",
      0, 0, 0, 0);

  public static final BattleBlock REDSTONE_BLOCK = block(
      BattleBlockIDs.REDSTONE_BLOCK, "Block of Redstone",
      "Reactivates all adjacent blocks.",
      -20, -5, -20, 0);

  public static final BattleBlock DIAMOND_BLOCK = block(
      BattleBlockIDs.DIAMOND_BLOCK, "Block of Diamond",
      "After placing it, it is replaced by Dirt in your deck and on the board.",
      24, 4, 24, 0);

  public static final BattleBlock COPPER_BLOCK = block(
      BattleBlockIDs.COPPER_BLOCK, "Copper Block",
      "On each of your turns after this one, it oxidises one stage further. Each extra stage gives +1 defence per turn.",
      0, false,
      0, true,
      0, false,
      0, false);

  public static final BattleBlock CHISELED_COPPER = block(
      BattleBlockIDs.CHISELED_COPPER, "Chiseled Copper",
      "On each of your turns after this one, it oxidises one stage further. Each extra stage makes it deal +3 damage when a block is placed next to it, then it breaks.",
      0, 0, 0, 0);

  public static final BattleBlock COPPER_GRATE = block(
      BattleBlockIDs.COPPER_GRATE, "Copper Grate",
      "On each of your turns after this one, it oxidises one stage further. Each extra stage adds +1 per-turn damage.",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock COPPER_BULB = block(
      BattleBlockIDs.COPPER_BULB, "Copper Bulb",
      "On each of your turns after this one, it oxidises one stage further. Each extra stage adds +1 per-turn healing.",
      0, false,
      0, false,
      5, true,
      0, false);

  public static final BattleBlock COPPER_LANTERN = block(
      BattleBlockIDs.COPPER_LANTERN, "Copper Lantern",
      "None",
      0, 0, 0, 0);

  public static final BattleBlock OBSIDIAN = block(
      BattleBlockIDs.OBSIDIAN, "Obsidian",
      "Makes adjacent blocks immune to explosions.",
      0, 0, 0, 0);

  public static final BattleBlock CRYING_OBSIDIAN = block(
      BattleBlockIDs.CRYING_OBSIDIAN, "Crying Obsidian",
      "Breaks adjacent Otherworldly blocks and stores the friendly ones it broke after activating them.",
      0, 0, 0, 0);

  public static final BattleBlock BEDROCK = block(
      BattleBlockIDs.BEDROCK, "Bedrock",
      "Unbreakable. Miss your next turn.",
      0, 12, 0, 0);

  public static final BattleBlock LAVA = block(
      BattleBlockIDs.LAVA, "Lava",
      "Deals 2 more damage each turn, up to 12.",
      2, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock TNT = block(
      BattleBlockIDs.TNT, "TNT",
      "Breaks itself and the 4 adjacent blocks.",
      0, 0, 0, 0);

  public static final BattleBlock POINTED_DRIPSTONE = block(
      BattleBlockIDs.POINTED_DRIPSTONE, "Pointed Dripstone",
      "Deals +8 damage if placed upside down.",
      6, 0, 0, 0);

  public static final BattleBlock RED_CARPET = block(
      BattleBlockIDs.RED_CARPET, "Red Carpet",
      "Doubles damage of the block underneath.",
      0, 0, 0, 0);

  public static final BattleBlock BLUE_CARPET = block(
      BattleBlockIDs.BLUE_CARPET, "Blue Carpet",
      "Doubles defence of the block underneath.",
      0, 0, 0, 0);

  public static final BattleBlock GREEN_CARPET = block(
      BattleBlockIDs.GREEN_CARPET, "Green Carpet",
      "Doubles healing of the block underneath.",
      0, 0, 0, 0);

  public static final BattleBlock RED_BED = block(
      BattleBlockIDs.RED_BED, "Red Bed",
      "Activate 1 turn of your blocks' per-turn effects when placed. If Night Warp is active, reset it to None.",
      0, 0, 8, 0);

  public static final BattleBlock CAKE = block(
      BattleBlockIDs.CAKE, "Cake",
      "Breaks after 4 turns.",
      -10, false,
      0, false,
      12, true,
      0, false);

  public static final BattleBlock MUSHROOM_STEM = requireOnGrassVariants(
      block(
          BattleBlockIDs.MUSHROOM_STEM, "Mushroom Stem",
          "Every turn, 1 Mushroom Stem gets placed above it if the block above is air.",
          6, true,
          0, false,
          0, false,
          0, false));

  public static final BattleBlock COCOA = requireOn(
      block(
          BattleBlockIDs.COCOA, "Cocoa Beans",
          "None",
          0, false,
          0, false,
          9, true,
          0, false),
      COCOA_LOG_REQUIREMENT,
      BattleBlockIDs.CHERRY_LOG,
      BattleBlockIDs.JUNGLE_LOG,
      BattleBlockIDs.PALE_OAK_LOG);

  public static final BattleBlock SLIME_BLOCK = block(
      BattleBlockIDs.SLIME_BLOCK, "Slime Block",
      "None",
      6, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock PLAYER_HEAD = block(
      BattleBlockIDs.PLAYER_HEAD, "Player Head",
      "Gives you an extra turn.",
      -8, -2, -10, 0);

  public static final BattleBlock CREEPER_HEAD = block(
      BattleBlockIDs.CREEPER_HEAD, "Creeper Head",
      "Breaks the block beneath it when placed, then after 1 turn explodes for 8 damage and breaks blocks within 1 block.",
      0, 0, 0, 0);

  public static final BattleBlock PIGLIN_HEAD = block(
      BattleBlockIDs.PIGLIN_HEAD, "Piglin Head",
      "If a golden block is broken adjacent to this, gain 4 defence and 10 healing, and deal 10 damage to your opponent.",
      10, 0, 0, 0);

  public static final BattleBlock SKELETON_SKULL = block(
      BattleBlockIDs.SKELETON_SKULL, "Skeleton Skull",
      "Breaks in sunlight after dealing damage.",
      18, true,
      0, false,
      -16, false,
      0, false);

  public static final BattleBlock WITHER_SKELETON_SKULL = block(
      BattleBlockIDs.WITHER_SKELETON_SKULL, "Wither Skeleton Skull",
      "If the opponent has more than 6 defence, deal +8 damage.",
      18, 0, 0, 0);

  public static final BattleBlock ZOMBIE_HEAD = block(
      BattleBlockIDs.ZOMBIE_HEAD, "Zombie Head",
      "Breaks in sunlight after dealing damage.",
      14, true,
      0, false,
      0, false,
      0, false);

  public static final BattleBlock DRAGON_HEAD = requireOn(
      block(
          BattleBlockIDs.DRAGON_HEAD, "Dragon Head",
          "None",
          46, 0, 0, 0),
      "Place on Dragon Egg.",
      BattleBlockIDs.DRAGON_EGG);

  public static final BattleBlock OAK_PLANKS = block(
      BattleBlockIDs.OAK_PLANKS, "Oak Planks",
      "Adds 4 Oak Planks to your next hand.",
      0, 0, 0, 0);

  public static final BattleBlock RAW_IRON_BLOCK = block(
      BattleBlockIDs.RAW_IRON_BLOCK, "Block of Raw Iron",
      "Deals 30 damage next turn.",
      0, 0, 0, 0);

  public static final BattleBlock RAW_GOLD_BLOCK = block(
      BattleBlockIDs.RAW_GOLD_BLOCK, "Block of Raw Gold",
      "Gives 6 defence next turn.",
      0, 0, 0, 0);

  public static final BattleBlock RAW_COPPER_BLOCK = block(
      BattleBlockIDs.RAW_COPPER_BLOCK, "Block of Raw Copper",
      "Heals 26 next turn.",
      0, 0, 0, 0);

  public static final BattleBlock PALE_OAK_LOG = block(
      BattleBlockIDs.PALE_OAK_LOG, "Pale Oak Log",
      "None",
      10, true,
      0, false,
      -5, true,
      0, false);

  public static final BattleBlock PALE_MOSS_BLOCK = block(
      BattleBlockIDs.PALE_MOSS_BLOCK, "Pale Moss Block",
      "Places Pale Moss Carpet on all adjacent blocks.",
      0, 0, 0, 0);

  public static final BattleBlock PALE_MOSS_CARPET = block(
      BattleBlockIDs.PALE_MOSS_CARPET, "Pale Moss Carpet",
      "Unchoosable. Reduces all stats of the block underneath by 2.",
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

  private static Classification classificationFor(BattleBlockIDs id) {
    return switch (id) {
      case CORNFLOWER, PRISMARINE, COBBLESTONE -> Classification.NONE;

      case GRASS_BLOCK,
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
          POWDER_SNOW,
          MOSS_BLOCK,
          DEAD_BUSH,
          CACTUS,
          RED_TULIP,
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
          CONDUIT,
          MUSHROOM_STEM,
          COCOA,
          SLIME_BLOCK,
          PLAYER_HEAD,
          CREEPER_HEAD,
          SKELETON_SKULL,
          ZOMBIE_HEAD,
          RAW_IRON_BLOCK,
          RAW_GOLD_BLOCK,
          RAW_COPPER_BLOCK,
          PALE_OAK_LOG,
          PALE_MOSS_BLOCK,
          PALE_MOSS_CARPET,
          CREAKING_HEART -> Classification.NATURAL;

      case NETHERRACK,
          CANDLE,
          CAMPFIRE,
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
          SMITHING_TABLE,
          REPEATER,
          DAYLIGHT_DETECTOR,
          REDSTONE_TORCH,
          TORCH,
          COPPER_TORCH,
          LIGHTNING_ROD,
          DISPENSER,
          SPAWNER,
          CHISELED_BOOKSHELF,
          BOOKSHELF,
          VAULT,
          SMOOTH_STONE,
          GLASS,
          GLASS_PANE,
          COPPER_BLOCK,
          CHISELED_COPPER,
          COPPER_GRATE,
          COPPER_BULB,
          COPPER_LANTERN,
          TNT,
          RED_CARPET,
          BLUE_CARPET,
          GREEN_CARPET,
          RED_BED,
          CAKE,
          OAK_PLANKS -> Classification.MAN_MADE;

      case NETHERITE_BLOCK,
          SCULK_SENSOR,
          CALIBRATED_SCULK_SENSOR,
          SCULK_SHRIEKER,
          SCULK_CATALYST,
          SCULK,
          DEEPSLATE,
          REINFORCED_DEEPSLATE,
          DEEPSLATE_BRICKS,
          DEEPSLATE_TILES,
          DEEPSLATE_GOLD_ORE,
          DEEPSLATE_REDSTONE_ORE,
          STONE_BRICKS,
          CRACKED_STONE_BRICKS,
          COAL_BLOCK,
          IRON_BLOCK,
          GOLD_BLOCK,
          EMERALD_BLOCK,
          LAPIS_BLOCK,
          REDSTONE_BLOCK,
          DIAMOND_BLOCK,
          BEDROCK,
          LAVA,
          POINTED_DRIPSTONE -> Classification.CAVE;

      case NETHER_GOLD_ORE,
          NETHER_QUARTZ_ORE,
          CRIMSON_NYLIUM,
          WARPED_NYLIUM,
          CRIMSON_HYPHAE,
          WARPED_HYPHAE,
          SOUL_SAND,
          GLOWSTONE,
          MAGMA_BLOCK,
          SOUL_TORCH,
          SOUL_LANTERN,
          SOUL_CAMPFIRE,
          WITHER_ROSE,
          NETHER_BRICKS,
          RESPAWN_ANCHOR,
          ANCIENT_DEBRIS,
          END_STONE,
          END_CRYSTAL,
          DRAGON_EGG,
          BREWING_STAND,
          ENCHANTING_TABLE,
          SHULKER_BOX,
          BEACON,
          POLISHED_BLACKSTONE_BRICKS,
          OBSIDIAN,
          CRYING_OBSIDIAN,
          PIGLIN_HEAD,
          WITHER_SKELETON_SKULL,
          DRAGON_HEAD -> Classification.OTHERWORLDLY;
    };
  }
}
