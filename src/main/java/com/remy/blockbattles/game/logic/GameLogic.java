package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.remy.blockbattles.BlockBattlesMod;
import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.BattleBlockIDs;
import com.remy.blockbattles.game.blocks.Classification;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.blocks.abilities.Abilities;
import com.remy.blockbattles.game.gui.BattleCardItems;
import com.remy.blockbattles.network.BattleBlockOutlinePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class GameLogic {
  private final BattleState battleState;
  private final DeckManager deckManager;
  private final Random random = new Random();

  private int pendingDamage;
  private int pendingDirectHealthDamage;
  private int pendingHealing;
  private int pendingMaxHealthGain;
  private int pendingShieldGain;
  private int pendingShieldDamage;

  public GameLogic() {
    this(BattleState.shared());
  }

  public GameLogic(BattleState battleState) {
    this.battleState = battleState;
    this.deckManager = new DeckManager();
  }

  public boolean isGameRunning() {
    return battleState.isGameRunning();
  }

  public BattleWarp getActiveWarp() {
    return battleState.getActiveWarp();
  }

  public boolean canPlaceBattleBlock(String blockId, TeamSide actingSide) {
    if (!battleState.isGameRunning()) {
      return true;
    }

    if (!isBattlePlacement(blockId)) {
      return true;
    }

    return actingSide != null
        && actingSide == battleState.getActiveSide()
        && battleState.getRemainingPlacementsThisTurn() > 0
        && battleState.getTeam(actingSide).hasCardInHand(blockId);
  }

  public boolean canPlaceBattleBlock(
      String blockId,
      TeamSide actingSide,
      String supportBlockId,
      TeamSide supportOwnerSide) {
    return getBattleBlockPlacementFailure(blockId, actingSide, supportBlockId, supportOwnerSide) == null;
  }

  public String getBattleBlockPlacementFailure(
      String blockId,
      TeamSide actingSide,
      String supportBlockId,
      TeamSide supportOwnerSide) {
    if (!battleState.isGameRunning()) {
      return null;
    }

    if (!isBattlePlacement(blockId)) {
      return null;
    }

    if (actingSide == null) {
      return "Join the Red or Blue team first with /BB join red or /BB join blue.";
    }

    if (actingSide != battleState.getActiveSide()) {
      return "It is " + battleState.getActiveSide().getDisplayName() + "'s turn.";
    }

    if (!battleState.getTeam(actingSide).hasCardInHand(blockId)) {
      return "That block is not in your current 3-card hand.";
    }

    if (battleState.getRemainingPlacementsThisTurn() <= 0) {
      return "You have already placed all of your blocks for this turn.";
    }

    if (isPlacementBlockedByWarp(blockId)) {
      return "That block cannot be placed during " + battleState.getActiveWarp().getDisplayName() + ".";
    }

    return null;
  }

  public TeamSide getPlacedBattleBlockOwner(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);
    return trackedBlock == null ? null : trackedBlock.ownerTeam().getSide();
  }

  public boolean onPlaceBattleBlock(String blockId) {
    return CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> placeBattleBlock(battleBlock, battleState.getActiveSide(), null, null))
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos) {
    return onPlaceBattleBlock(blockId, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos, TeamSide actingSide) {
    return CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> {
          if (level instanceof ServerLevel serverLevel) {
            return placeBattleBlock(battleBlock, actingSide, serverLevel, pos);
          }

          return placeBattleBlock(battleBlock, actingSide, null, null);
        })
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock) {
    return placeBattleBlock(battleBlock, battleState.getActiveSide(), null, null);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, TeamSide actingSide) {
    return placeBattleBlock(battleBlock, actingSide, null, null);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    return onPlaceBattleBlock(battleBlock, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    return placeBattleBlock(battleBlock, actingSide, level, pos);
  }

  public BattleState getBattleState() {
    return battleState;
  }

  public BattleTeam getCurrentTeam() {
    return battleState.getActiveTeam();
  }

  public BattleTeam getEnemyTeam() {
    return battleState.getWaitingTeam();
  }

  private BattleBlock applyOnPlaceAbility(
      BattleBlock battleBlock,
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      ServerLevel level,
      BlockPos pos) {
    if (level != null && pos != null && isActivationSuppressed(battleBlock, level, pos)) {
      return battleBlock;
    }

    int effectMultiplier = getBlockEffectMultiplier(level, pos, battleBlock);

    switch (battleBlock.id) {
      case TNT -> {
        if (!areExplosionsDisabled() && level != null && pos != null) {
          breakBlocksAround(level, pos);
          return null;
        }
      }
      case RED_TULIP -> {
        for (int i = 0; i < effectMultiplier; i++) {
          Abilities.redTulipAbility(currentTeam);
        }
      }
      case CORNFLOWER -> {
        queueHealingSource(currentTeam, (currentTeam.getHealth() / 4) * effectMultiplier, true);
      }
      case PINK_PETALS -> currentTeam.queueHealingAlsoIncreasesMaxHealth();
      case CARVED_PUMPKIN -> {
        if (level != null && pos != null) {
          queueHealingSource(currentTeam, countNearbyBlocks(level, pos, 1) * 4 * effectMultiplier, true);
        }
      }
      case CHERRY_LEAVES -> {
        queueHealingSource(currentTeam, (currentTeam.getMaxHealth() / 10) * effectMultiplier, true);
      }
      case MOSS_BLOCK -> {
        if (level != null && pos != null) {
          replaceNearbyBlocksWithGrass(level, pos, 1);
        }
      }
      case NETHERRACK -> {
        queueDamageSource(currentTeam, currentTeam.getLastDamageDealt() * effectMultiplier, true);
      }
      case VAULT -> {
        if (currentTeam.removeOneCardFromDeck(CreateBlocks.VAULT)) {
          queueShieldSource(currentTeam, 8 * effectMultiplier, true);
          queueHealingSource(currentTeam, 8 * effectMultiplier, true);
        }
      }
      case SCULK -> enemyTeam.increaseMaxHealth(-2 * effectMultiplier);
      case END_CRYSTAL -> {
        if (!areExplosionsDisabled() && level != null && pos != null) {
          breakBlocksInCube(level, pos, 2, true);
          return null;
        }
      }
      case FURNACE -> {
        if (level != null && pos != null) {
          queueShieldSource(currentTeam, countNearbyBlocks(level, pos, 1) * effectMultiplier, true);
        }
      }
      case LAPIS_BLOCK -> {
        queueHealingSource(currentTeam, ((currentTeam.getHealth() + 1) / 2) * effectMultiplier, true);
        currentTeam.replaceOneCardInDeck(CreateBlocks.LAPIS_BLOCK, CreateBlocks.DIRT);

        if (level != null && pos != null) {
          level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }

        return CreateBlocks.DIRT;
      }
      case DEEPSLATE_BRICKS -> {
        queueDamageSource(currentTeam, currentTeam.getShield() * 2 * effectMultiplier, true);
      }
      case REINFORCED_DEEPSLATE -> {
        queueShieldSource(currentTeam, Math.min(currentTeam.getShield(), 10) * effectMultiplier, true);
      }
      case WITHER_SKELETON_SKULL -> {
        if (enemyTeam.getShield() > 6) {
          queueDamageSource(currentTeam, 8 * effectMultiplier, true);
        }
      }
      case WITHER_ROSE -> {
        if (level != null && pos != null) {
          int replacedGrassBlocks = replaceNearbyGrassWithSoulSand(level, pos, 1);

          if (replacedGrassBlocks > 0) {
            queueDamageSource(currentTeam, replacedGrassBlocks * 3 * effectMultiplier, true);
            queueHealingSource(currentTeam, replacedGrassBlocks * effectMultiplier, true);
          }
        }
      }
      case LECTERN -> {
        queueDamageSource(currentTeam, currentTeam.getHandSize() * effectMultiplier, true);
      }
      case SMITHING_TABLE -> {
        queueDamageSource(currentTeam, countBlocksOnBoard(Classification.MAN_MADE, battleBlock) * effectMultiplier, true);
      }
      case SOUL_TORCH -> currentTeam.queueTurnHealingBonus(4 * effectMultiplier);
      case REDSTONE_TORCH -> currentTeam.queueTurnDamageBonus(5 * effectMultiplier);
      case TORCH -> currentTeam.queueTurnShieldBonus(2 * effectMultiplier);
      case COPPER_TORCH -> currentTeam.queueIgnoreNextIncomingDamage();
      case CRIMSON_HYPHAE -> {
        queueDamageSource(currentTeam, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock) * effectMultiplier, true);
      }
      case WARPED_HYPHAE -> {
        queueHealingSource(currentTeam, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock) * effectMultiplier, true);
      }
      case RAW_IRON_BLOCK -> currentTeam.queueTurnDamage(30 * effectMultiplier);
      case RAW_GOLD_BLOCK -> currentTeam.queueTurnShieldGain(6 * effectMultiplier);
      case RAW_COPPER_BLOCK -> currentTeam.queueTurnHealing(26 * effectMultiplier);
      case NETHERITE_BLOCK -> {
        currentTeam.replaceOneCardInDeck(CreateBlocks.NETHERITE_BLOCK, CreateBlocks.DIRT);

        if (level != null && pos != null) {
          level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }

        return CreateBlocks.DIRT;
      }
      case CHEST -> {
        if (level != null && pos != null) {
          activateChestAbility(currentTeam, enemyTeam, level, pos);
        }
      }
      case TRAPPED_CHEST -> {
        if (level != null && pos != null) {
          activateTrappedChestAbility(currentTeam, level, pos);
        }
      }
      case ENCHANTING_TABLE -> {
        if (level != null && pos != null) {
          transferAdjacentBlocksToTeam(currentTeam, level, pos);
        }
      }
      case REPEATER -> {
        if (level != null && pos != null) {
          reactivateBlockBelow(level, pos);
        }
      }
      case POINTED_DRIPSTONE -> {
        if (level != null && pos != null && Abilities.isPointedDripstoneUpsideDown(level, pos)) {
          queueDamageSource(currentTeam, 8 * effectMultiplier, true);
        }
      }
      case SOUL_SAND -> enemyTeam.queueTurnDrawModifier(-1 * effectMultiplier);
      case RED_BED -> {
        if (doBedsExplodeOnPlacement()) {
          if (level != null && pos != null) {
            breakBlocksAround(level, pos);
          }

          return null;
        }
      }
      default -> {
      }
    }

    return battleBlock;
  }

  private boolean placeBattleBlock(BattleBlock battleBlock, TeamSide actingSide, ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return false;
    }

    if (!canPlaceBattleBlock(battleBlock.id.getId(), actingSide)) {
      return false;
    }

    TeamSide resolvedActingSide = resolveActingSide(actingSide);
    BattleTeam currentTeam = getTeamForTurn(resolvedActingSide);
    BattleTeam enemyTeam = getEnemyTeamForTurn(resolvedActingSide);

    if (!currentTeam.removeOneCardFromHand(battleBlock)) {
      return false;
    }

    BattleBlock placedBlock = applyOnPlaceAbility(battleBlock, currentTeam, enemyTeam, level, pos);

    queueImmediateBlockEffects(battleBlock, currentTeam, level, pos);

    if (level != null && pos != null && placedBlock != null && isWarpActive(BattleWarp.REDSTONE)) {
      activatePlacedBlockNow(new OwnedPlacedBattleBlock(
          currentTeam,
          new PlacedBattleBlock(level, pos, placedBlock)));
    }

    boolean shouldEndTurn = battleState.consumePlacementThisTurn() <= 0;

    if (shouldEndTurn) {
      endTurn(resolvedActingSide);
    }

    if (level != null && pos != null && placedBlock != null) {
      registerPlacedBlock(placedBlock, level, pos, resolvedActingSide);
    }

    return true;
  }

  private void queueImmediateBlockEffects(BattleBlock battleBlock, BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    if (level != null && pos != null && isActivationSuppressed(battleBlock, level, pos)) {
      return;
    }

    if (isCoralSuppressedByWarp(battleBlock.id)) {
      return;
    }

    if ((battleBlock.id == BattleBlockIDs.RED_BED && doBedsExplodeOnPlacement())
        || (battleBlock.id == BattleBlockIDs.TNT && areExplosionsDisabled())
        || (battleBlock.id == BattleBlockIDs.END_CRYSTAL && areExplosionsDisabled())) {
      return;
    }

    int statBonus = getPodzolStatBonus(currentTeam, level, pos);
    int damage = battleBlock.damage + statBonus;
    int healing = battleBlock.healing + statBonus;
    int shield = battleBlock.defence + statBonus;
    int shieldDamage = battleBlock.defenceDamage + statBonus;

    if (isWarpActive(BattleWarp.BED_WARS)) {
      if (battleBlock.id == BattleBlockIDs.OAK_PLANKS) {
        shield += 1;
      } else if (battleBlock.id == BattleBlockIDs.OBSIDIAN) {
        shield += 2;
      } else if (battleBlock.id == BattleBlockIDs.GLASS) {
        shield += 3;
      }
    }

    int effectMultiplier = getBlockEffectMultiplier(level, pos, battleBlock);
    damage *= effectMultiplier;
    healing *= effectMultiplier;
    shield *= effectMultiplier;
    shieldDamage *= effectMultiplier;

    if (!battleBlock.damagePerTurn && damage != 0) {
      if (battleBlock.id == BattleBlockIDs.SOUL_LANTERN) {
        queueDirectHealthDamageSource(currentTeam, damage, false);
      } else {
        queueDamageSource(currentTeam, damage, false);
      }
    }

    if (!battleBlock.healingPerTurn && healing != 0) {
      queueHealingSource(currentTeam, healing, false);
    }

    if (!battleBlock.defencePerTurn && shield != 0) {
      queueShieldSource(currentTeam, shield, false);
    }

    if (!battleBlock.defenceDamagePerTurn && shieldDamage != 0) {
      pendingShieldDamage += adjustShieldDamageAmount(shieldDamage);
    }
  }

  private void registerPlacedBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    getTeamForTurn(actingSide).addPlacedBlock(new PlacedBattleBlock(level, pos, battleBlock));
  }

  private boolean hasPerTurnEffect(BattleBlock battleBlock) {
    return battleBlock.damagePerTurn
        || battleBlock.healingPerTurn
        || battleBlock.defencePerTurn
        || battleBlock.defenceDamagePerTurn;
  }

  private void queueDamageSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      pendingDamage += adjustDamageAmount(amount + currentTeam.getActiveTurnDamageBonus());
    }
  }

  private void queueDirectHealthDamageSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      pendingDirectHealthDamage += adjustDamageAmount(amount + currentTeam.getActiveTurnDamageBonus());
    }
  }

  private void queueHealingSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int totalHealing = adjustHealingAmount(amount + currentTeam.getActiveTurnHealingBonus());
      pendingHealing += totalHealing;

      if (currentTeam.isActiveHealingAlsoIncreasesMaxHealth() && totalHealing > 0) {
        pendingMaxHealthGain += totalHealing;
      }

      if (isWarpActive(BattleWarp.LUSH_CAVE) && totalHealing > 0) {
        pendingDamage += totalHealing;
      }
    }
  }

  private void queueShieldSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      pendingShieldGain += adjustShieldGainAmount(amount + currentTeam.getActiveTurnShieldBonus());
    }
  }

  private boolean isBattlePlacement(String blockId) {
    return blockId.equals(BattleBlockIDs.TNT.getId())
        || CreateBlocks.findByMinecraftId(blockId).isPresent();
  }

  private TeamSide resolveActingSide(TeamSide actingSide) {
    return actingSide == null ? battleState.getActiveSide() : actingSide;
  }

  private BattleTeam getTeamForTurn(TeamSide actingSide) {
    return battleState.getTeam(resolveActingSide(actingSide));
  }

  private BattleTeam getEnemyTeamForTurn(TeamSide actingSide) {
    return battleState.getOpponentOf(resolveActingSide(actingSide));
  }

  private void queuePerTurnEffects(BattleTeam currentTeam, BattleTeam enemyTeam) {
    if (arePerTurnBlocksDisabled()) {
      return;
    }

    ArrayList<PlacedBattleBlock> grownBlocks = new ArrayList<>();
    ArrayList<PlacedBattleBlock> placedBlocksSnapshot = new ArrayList<>(currentTeam.getPlacedBlocks());

    for (PlacedBattleBlock placedBlock : placedBlocksSnapshot) {
      if (!currentTeam.getPlacedBlocks().contains(placedBlock)) {
        continue;
      }

      if (!refreshPlacedBlock(currentTeam, enemyTeam, placedBlock)) {
        currentTeam.getPlacedBlocks().remove(placedBlock);
        continue;
      }

      if (!currentTeam.getPlacedBlocks().contains(placedBlock) || !canActivatePlacedBlock(currentTeam, placedBlock)) {
        continue;
      }

      BattleBlock battleBlock = placedBlock.battleBlock();

      if (battleBlock.damagePerTurn) {
        int damageAmount = getPerTurnDamageAmount(placedBlock, currentTeam);

        if (battleBlock.id == BattleBlockIDs.SOUL_CAMPFIRE) {
          queueDirectHealthDamageSource(currentTeam, damageAmount, false);
        } else {
          queueDamageSource(currentTeam, damageAmount, false);
        }

        if (battleBlock.id == BattleBlockIDs.MYCELIUM) {
          queueHealingSource(currentTeam, Math.max(0, damageAmount - enemyTeam.getShield()), false);
        }
      }

      if (battleBlock.healingPerTurn) {
        queueHealingSource(currentTeam, getPerTurnHealingAmount(placedBlock, currentTeam), false);
      }

      if (battleBlock.defencePerTurn) {
        queueShieldSource(currentTeam, getPerTurnShieldAmount(placedBlock, currentTeam), false);
      }

      if (battleBlock.defenceDamagePerTurn) {
        pendingShieldDamage += adjustShieldDamageAmount(getPerTurnShieldDamageAmount(placedBlock, currentTeam));
      }

      applyPerTurnAbility(currentTeam, enemyTeam, placedBlock, grownBlocks);
    }

    grownBlocks.forEach(currentTeam::addPlacedBlock);
  }

  private void applyPerTurnAbility(
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      PlacedBattleBlock placedBlock,
      ArrayList<PlacedBattleBlock> grownBlocks) {
    switch (placedBlock.battleBlock().id) {
      case POWDER_SNOW -> {
        if (placedBlock.advanceOwnerTurnCount() == 1) {
          queueDamageSource(
              currentTeam,
              applyCarpetModifier(placedBlock, 4, BattleBlockIDs.RED_CARPET),
              true);
        }
      }
      case CHERRY_LOG, JUNGLE_LOG, MUSHROOM_STEM -> {
        if (Abilities.growBlockUpwardIfAir(placedBlock.level(), placedBlock.pos())) {
          PlacedBattleBlock grownBlock = new PlacedBattleBlock(
              placedBlock.level(),
              placedBlock.pos().above(),
              placedBlock.battleBlock());
          grownBlocks.add(grownBlock);
          onAnyBlockPlaced(placedBlock.level(), grownBlock.pos());
        }
      }
      case CAMPFIRE -> {
        if (placedBlock.advanceOwnerTurnCount() >= 3) {
          currentTeam.getPlacedBlocks().remove(placedBlock);
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
          onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
        }
      }
      case DRAGON_EGG -> {
        if (placedBlock.advanceOwnerTurnCount() >= 2) {
          queueHealingSource(currentTeam, 32, true);
          currentTeam.getPlacedBlocks().remove(placedBlock);
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
          onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
        }
      }
      case TORCHFLOWER -> {
        if (placedBlock.advanceOwnerTurnCount() >= 1) {
          breakBattleBlockAt(placedBlock.level(), placedBlock.pos().below());
          currentTeam.getPlacedBlocks().remove(placedBlock);
          onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.LAVA.defaultBlockState(), 3);
          PlacedBattleBlock lavaBlock = new PlacedBattleBlock(placedBlock.level(), placedBlock.pos(), CreateBlocks.LAVA);
          grownBlocks.add(lavaBlock);
          onAnyBlockPlaced(placedBlock.level(), placedBlock.pos());
        }
      }
      case BREWING_STAND -> applyBrewingStandEffect(currentTeam, enemyTeam, placedBlock);
      case DISPENSER -> breakBattleBlockAt(
          placedBlock.level(),
          placedBlock.pos().relative(getDispenserFacing(placedBlock)));
      default -> {
      }
    }
  }

  private void queueQueuedTurnEffects(BattleTeam currentTeam) {
    queueDamageSource(currentTeam, currentTeam.consumeActiveTurnDamage(), false);
    queueHealingSource(currentTeam, currentTeam.consumeActiveTurnHealing(), false);
    queueShieldSource(currentTeam, currentTeam.consumeActiveTurnShieldGain(), false);
  }

  private void applyBrewingStandEffect(BattleTeam currentTeam, BattleTeam enemyTeam, PlacedBattleBlock placedBlock) {
    switch (random.nextInt(6)) {
      case 0 -> {
        dealImmediateDamage(currentTeam, enemyTeam, 5);
        dealDamageToTeam(currentTeam, 2);
      }
      case 1 -> applyImmediateHealing(currentTeam, enemyTeam, currentTeam.getLastDamageTakenFromOpponent() / 4);
      case 2 -> applyImmediateShieldGain(currentTeam, 6);
      case 3 -> currentTeam.queueTurnDrawModifier(2);
      case 4 -> enemyTeam.queueTurnDrawModifier(-1);
      case 5 -> {
        breakBlocksInCube(placedBlock.level(), placedBlock.pos(), 1, true);
        dealImmediateDamage(currentTeam, enemyTeam, 5);
        dealDamageToTeam(currentTeam, 5);
      }
      default -> {
      }
    }
  }

  private Direction getDispenserFacing(PlacedBattleBlock placedBlock) {
    if (placedBlock.level().getBlockState(placedBlock.pos()).hasProperty(DispenserBlock.FACING)) {
      return placedBlock.level().getBlockState(placedBlock.pos()).getValue(DispenserBlock.FACING);
    }

    return Direction.NORTH;
  }

  private boolean canActivatePlacedBlock(BattleTeam ownerTeam, PlacedBattleBlock placedBlock) {
    BattleBlock battleBlock = placedBlock.battleBlock();

    if (isActivationSuppressed(battleBlock, placedBlock.level(), placedBlock.pos())) {
      return false;
    }

    if (isCoralSuppressedByWarp(battleBlock.id)) {
      return false;
    }

    return switch (battleBlock.id) {
      case DAYLIGHT_DETECTOR -> isInSunlight(placedBlock.level(), placedBlock.pos());
      case SPAWNER -> !isInSunlight(placedBlock.level(), placedBlock.pos());
      default -> true;
    };
  }

  private boolean isActivationSuppressed(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    if (isWarpActive(BattleWarp.VILLAGE_HOUSE) && isMobHead(battleBlock.id)) {
      return true;
    }

    return isMobHeadOrSpawner(battleBlock.id) && hasNearbyBlock(level, pos, 2, BattleBlockIDs.CANDLE);
  }

  private boolean isMobHeadOrSpawner(BattleBlockIDs battleBlockId) {
    return battleBlockId == BattleBlockIDs.SPAWNER || isMobHead(battleBlockId);
  }

  private boolean isMobHead(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case CREEPER_HEAD,
          PIGLIN_HEAD,
          SKELETON_SKULL,
          WITHER_SKELETON_SKULL,
          ZOMBIE_HEAD,
          DRAGON_HEAD -> true;
      default -> false;
    };
  }

  private boolean isInSunlight(ServerLevel level, BlockPos pos) {
    if (battleState.getActiveWarp().overridesSunlight()) {
      return battleState.getActiveWarp().hasSunlight();
    }

    return level.dimensionType().hasSkyLight()
        && level.getSkyDarken() < 4
        && level.canSeeSky(pos.above());
  }

  private int getPodzolStatBonus(BattleTeam team, ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return 0;
    }

    return countNearbyOwnedBlocks(team, level, pos, 1, BattleBlockIDs.PODZOL);
  }

  private int getImmediateDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int damageAmount = placedBlock.battleBlock().damage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
    damageAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(
        placedBlock,
        damageAmount,
        BattleBlockIDs.RED_CARPET);
  }

  private int getImmediateHealingAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int healingAmount = placedBlock.battleBlock().healing + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
    healingAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(
        placedBlock,
        healingAmount,
        BattleBlockIDs.GREEN_CARPET);
  }

  private int getImmediateShieldAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int shieldAmount = placedBlock.battleBlock().defence + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (isWarpActive(BattleWarp.BED_WARS)) {
      if (placedBlock.battleBlock().id == BattleBlockIDs.OAK_PLANKS) {
        shieldAmount += 1;
      } else if (placedBlock.battleBlock().id == BattleBlockIDs.OBSIDIAN) {
        shieldAmount += 2;
      } else if (placedBlock.battleBlock().id == BattleBlockIDs.GLASS) {
        shieldAmount += 3;
      }
    }

    shieldAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(
        placedBlock,
        shieldAmount,
        BattleBlockIDs.BLUE_CARPET);
  }

  private int getImmediateShieldDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int shieldDamageAmount = placedBlock.battleBlock().defenceDamage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
    return shieldDamageAmount * getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());
  }

  private void activatePlacedBlockNow(OwnedPlacedBattleBlock trackedBlock) {
    BattleTeam ownerTeam = trackedBlock.ownerTeam();
    BattleTeam enemyTeam = battleState.getOpponentOf(ownerTeam.getSide());
    PlacedBattleBlock placedBlock = trackedBlock.placedBlock();

    if (!canActivatePlacedBlock(ownerTeam, placedBlock)) {
      return;
    }

    applyOnPlaceAbility(placedBlock.battleBlock(), ownerTeam, enemyTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().damagePerTurn) {
      int damageAmount = getPerTurnDamageAmount(placedBlock, ownerTeam);

      if (placedBlock.battleBlock().id == BattleBlockIDs.SOUL_CAMPFIRE) {
        queueDirectHealthDamageSource(ownerTeam, damageAmount, false);
      } else {
        queueDamageSource(ownerTeam, damageAmount, false);
      }

      if (placedBlock.battleBlock().id == BattleBlockIDs.MYCELIUM) {
        queueHealingSource(ownerTeam, Math.max(0, damageAmount - enemyTeam.getShield()), false);
      }
    } else {
      int damageAmount = getImmediateDamageAmount(placedBlock, ownerTeam);

      if (damageAmount != 0) {
        if (placedBlock.battleBlock().id == BattleBlockIDs.SOUL_LANTERN) {
          queueDirectHealthDamageSource(ownerTeam, damageAmount, false);
        } else {
          queueDamageSource(ownerTeam, damageAmount, false);
        }
      }
    }

    if (placedBlock.battleBlock().healingPerTurn) {
      queueHealingSource(ownerTeam, getPerTurnHealingAmount(placedBlock, ownerTeam), false);
    } else {
      int healingAmount = getImmediateHealingAmount(placedBlock, ownerTeam);

      if (healingAmount != 0) {
        queueHealingSource(ownerTeam, healingAmount, false);
      }
    }

    if (placedBlock.battleBlock().defencePerTurn) {
      queueShieldSource(ownerTeam, getPerTurnShieldAmount(placedBlock, ownerTeam), false);
    } else {
      int shieldAmount = getImmediateShieldAmount(placedBlock, ownerTeam);

      if (shieldAmount != 0) {
        queueShieldSource(ownerTeam, shieldAmount, false);
      }
    }

    if (placedBlock.battleBlock().defenceDamagePerTurn) {
      pendingShieldDamage += adjustShieldDamageAmount(getPerTurnShieldDamageAmount(placedBlock, ownerTeam));
    } else {
      int shieldDamageAmount = getImmediateShieldDamageAmount(placedBlock, ownerTeam);

      if (shieldDamageAmount != 0) {
        pendingShieldDamage += adjustShieldDamageAmount(shieldDamageAmount);
      }
    }
  }

  private void activateChestAbility(BattleTeam currentTeam, BattleTeam enemyTeam, ServerLevel level, BlockPos pos) {
    ArrayList<OwnedPlacedBattleBlock> nearbyBlocks = getTrackedBlocksInCube(level, pos, 1, false);
    ArrayList<OwnedPlacedBattleBlock> copiedFriendlyBlocks = new ArrayList<>();

    for (OwnedPlacedBattleBlock trackedBlock : nearbyBlocks) {
      if (trackedBlock.placedBlock().battleBlock().classification != Classification.MAN_MADE) {
        continue;
      }

      if (trackedBlock.ownerTeam() == currentTeam) {
        copiedFriendlyBlocks.add(trackedBlock);
      }

      breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
    }

    for (OwnedPlacedBattleBlock copiedFriendlyBlock : copiedFriendlyBlocks) {
      activatePlacedBlockNow(new OwnedPlacedBattleBlock(currentTeam, copiedFriendlyBlock.placedBlock()));
    }
  }

  private void activateTrappedChestAbility(BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    ArrayList<OwnedPlacedBattleBlock> nearbyBlocks = getTrackedBlocksInCube(level, pos, 1, false);

    for (OwnedPlacedBattleBlock trackedBlock : nearbyBlocks) {
      if (trackedBlock.placedBlock().battleBlock().classification != Classification.MAN_MADE) {
        continue;
      }

      if (trackedBlock.ownerTeam() == currentTeam) {
        removeTrackedBlockSilently(level, trackedBlock.placedBlock().pos());
        level.setBlock(trackedBlock.placedBlock().pos(), Blocks.AIR.defaultBlockState(), 3);
        onAnyBlockBroken(level, trackedBlock.placedBlock().pos());
        continue;
      }

      breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
    }
  }

  private void transferAdjacentBlocksToTeam(BattleTeam targetTeam, ServerLevel level, BlockPos pos) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 1, false)) {
      if (!areAdjacent(trackedBlock.placedBlock().pos(), pos) || trackedBlock.ownerTeam() == targetTeam) {
        continue;
      }

      trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
      targetTeam.addPlacedBlock(trackedBlock.placedBlock());
    }
  }

  private void reactivateBlockBelow(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos.below());

    if (trackedBlock != null) {
      activatePlacedBlockNow(trackedBlock);
    }
  }

  private int countBlocksOnBoard(Classification classification, BattleBlock additionalBlock) {
    int count = countBlocksOnBoard(battleState.getRedTeam(), classification)
        + countBlocksOnBoard(battleState.getBlueTeam(), classification);

    if (additionalBlock != null && additionalBlock.classification == classification) {
      count++;
    }

    return count;
  }

  private int countExistingBlocksOnBoard(Classification classification) {
    return countExistingBlocksOnBoard(battleState.getRedTeam(), classification)
        + countExistingBlocksOnBoard(battleState.getBlueTeam(), classification);
  }

  private int countExistingBlocksOnBoard(BattleTeam team, Classification classification) {
    int count = 0;

    for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
      if (placedBlock.stillExists() && placedBlock.battleBlock().classification == classification) {
        count++;
      }
    }

    return count;
  }

  private int countBlocksOnBoard(BattleTeam team, Classification classification) {
    int count = 0;
    Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!refreshPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock)) {
        iterator.remove();
        continue;
      }

      if (placedBlock.battleBlock().classification == classification) {
        count++;
      }
    }

    return count;
  }

  public void endTurn() {
    endTurn(battleState.getActiveSide());
  }

  public void endTurn(TeamSide actingSide) {
    if (!battleState.isGameRunning()) {
      return;
    }

    TeamSide resolvedActingSide = resolveActingSide(actingSide);
    BattleTeam currentTeam = getTeamForTurn(resolvedActingSide);
    BattleTeam enemyTeam = getEnemyTeamForTurn(resolvedActingSide);

    applyStartOfTurnWarpCosts(currentTeam, enemyTeam);
    queuePerTurnEffects(currentTeam, enemyTeam);
    queueQueuedTurnEffects(currentTeam);

    if (pendingMaxHealthGain > 0) {
      currentTeam.increaseMaxHealth(pendingMaxHealthGain);
    }

    applyImmediateHealing(currentTeam, enemyTeam, pendingHealing);
    applyImmediateShieldGain(currentTeam, pendingShieldGain);

    int actualDamageDealt = 0;

    if (enemyTeam.consumeIgnoreNextIncomingDamage()) {
      pendingDamage = 0;
      pendingDirectHealthDamage = 0;
    }

    if (shouldIgnoreDefenceForDamage()) {
      pendingDirectHealthDamage += pendingDamage;
      pendingDamage = 0;
    }

    actualDamageDealt += enemyTeam.takeHealthDamage(pendingDamage);
    actualDamageDealt += enemyTeam.takeDirectHealthDamage(pendingDirectHealthDamage);
    enemyTeam.loseShield(pendingShieldDamage);
    tryReviveWithBed(enemyTeam);
    currentTeam.recordLastDamageDealt(actualDamageDealt);
    enemyTeam.recordLastDamageTakenFromOpponent(actualDamageDealt);
    applyNetherGoldOreRetaliation(enemyTeam, actualDamageDealt);
    applyCactusReflection(enemyTeam, currentTeam, actualDamageDealt);
    applyEndOfTurnWarpCosts(currentTeam, enemyTeam);

    currentTeam.clearActiveTurnEffects();
    clearPendingEffects();
    currentTeam.clearHand();
    battleState.setActiveSide(resolvedActingSide.otherSide());
    beginActiveTurn();
  }

  public void resetBattle() {
    clearPendingEffects();
    battleState.clearPlacedBlocks();
    battleState.resetForNewBattle();
    battleState.getRedTeam().clearHand();
    battleState.getBlueTeam().clearHand();

    if (!battleState.isGameRunning()) {
      return;
    }

    beginActiveTurn();
  }

  public void startGame() {
    battleState.setGameRunning(true);
    resetBattle();
  }

  public void endGame() {
    battleState.setGameRunning(false);
    resetBattle();
  }

  public void updateConfiguredDeck(TeamSide side, java.util.List<BattleBlock> deck) {
    battleState.setConfiguredDeck(side, deck);
    battleState.applyConfiguredDeck(side);

    if (!battleState.isGameRunning()) {
      battleState.getRedTeam().clearHand();
      battleState.getBlueTeam().clearHand();
      return;
    }

    if (battleState.getActiveSide() == side) {
      battleState.getOpponentOf(side).clearHand();
      drawHandForTeam(battleState.getActiveTeam());
      return;
    }

    battleState.getTeam(side).clearHand();
  }

  public void syncBattleHands(MinecraftServer server) {
    BattleCardItems.syncHands(server, battleState);
  }

  public void syncTrackedBattleBlocks(MinecraftServer server) {
    BattleBlockOutlinePayload payload = createBattleBlockOutlinePayload();

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      syncTrackedBattleBlocks(player, payload);
    }
  }

  public void syncTrackedBattleBlocks(ServerPlayer player) {
    syncTrackedBattleBlocks(player, createBattleBlockOutlinePayload());
  }

  public void forceTurn(TeamSide side) {
    if (!battleState.isGameRunning()) {
      return;
    }

    battleState.getRedTeam().clearHand();
    battleState.getBlueTeam().clearHand();
    battleState.getRedTeam().clearActiveTurnEffects();
    battleState.getBlueTeam().clearActiveTurnEffects();
    battleState.setActiveSide(side);
    beginActiveTurn();
  }

  public void redrawActiveHand() {
    if (!battleState.isGameRunning()) {
      return;
    }

    drawHandForTeam(battleState.getActiveTeam());
  }

  public void setTeamHealth(TeamSide side, int health) {
    battleState.getTeam(side).setHealth(health);
  }

  public void setTeamMaxHealth(TeamSide side, int maxHealth) {
    battleState.getTeam(side).setMaxHealth(maxHealth);
  }

  public void setTeamShield(TeamSide side, int shield) {
    battleState.getTeam(side).setShield(shield);
  }

  public void drawCardsForTeam(TeamSide side, int amount) {
    deckManager.dealCards(battleState.getTeam(side), Math.max(0, amount));
  }

  public void addCardToHand(TeamSide side, BattleBlock battleBlock) {
    BattleTeam team = battleState.getTeam(side);

    if (!team.removeOneCardFromDrawPile(battleBlock)) {
      team.addCardToHand(battleBlock);
      return;
    }

    team.addCardToHand(battleBlock);
  }

  public void clearHand(TeamSide side) {
    battleState.getTeam(side).clearHand();
  }

  public void refillDrawPile(TeamSide side) {
    BattleTeam team = battleState.getTeam(side);
    team.refillDrawPile();
    team.shuffleDrawPile();
  }

  private void beginActiveTurn() {
    BattleTeam activeTeam = battleState.getActiveTeam();
    activeTeam.activateQueuedTurnEffects();

    if (advanceWarpRoundAndMaybeFinishGame(activeTeam.getSide())) {
      return;
    }

    int placementsThisTurn = getPlacementsPerTurnForNewTurn();
    battleState.setRemainingPlacementsThisTurn(placementsThisTurn);
    drawHandForTeam(activeTeam, getBaseHandSizeForNewTurn());

    if (battleState.getActiveWarp() != BattleWarp.NONE) {
      battleState.advanceActiveWarpTurnCount();
    }
  }

  private void drawHandForTeam(BattleTeam team) {
    drawHandForTeam(team, getBaseHandSizeForCurrentTurn(team));
  }

  private void drawHandForTeam(BattleTeam team, int baseHandSize) {
    team.clearHand();

    if (isWarpActive(BattleWarp.LIBRARY)) {
      if (team.getDrawPile().isEmpty()) {
        team.refillDrawPile();
        team.shuffleDrawPile();
      }

      while (!team.getDrawPile().isEmpty()) {
        team.addCardToHand(team.getDrawPile().remove(0));
      }

      return;
    }

    deckManager.dealCards(team, Math.max(0,
        baseHandSize
            + team.getActiveTurnDrawModifier()
            + getWarpDrawModifier()
            + countBlocksOnBoard(team, BattleBlockIDs.SHULKER_BOX)
            - countBlocksOnBoard(battleState.getOpponentOf(team.getSide()), BattleBlockIDs.SNOW)));
  }

  private boolean advanceWarpRoundAndMaybeFinishGame(TeamSide activeSide) {
    if (!isWarpActive(BattleWarp.END) || battleState.getActiveWarpStarterSide() != activeSide) {
      return false;
    }

    battleState.advanceActiveWarpRoundCount();

    if (battleState.getActiveWarpRoundCount() < 3) {
      return false;
    }

    finishGameFromEndWarp(determineEndWarpLosingSide(activeSide));
    return true;
  }

  private TeamSide determineEndWarpLosingSide(TeamSide starterSide) {
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    if (redTeam.getHealth() == blueTeam.getHealth()) {
      return starterSide.otherSide();
    }

    return redTeam.getHealth() < blueTeam.getHealth()
        ? TeamSide.RED
        : TeamSide.BLUE;
  }

  private void finishGameFromEndWarp(TeamSide losingSide) {
    battleState.getTeam(losingSide).setHealth(0);

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      team.clearHand();
      team.clearQueuedTurnEffects();
      team.clearActiveTurnEffects();
    }

    clearPendingEffects();
    battleState.setRemainingPlacementsThisTurn(0);
    battleState.setGameRunning(false);
  }

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingDirectHealthDamage = 0;
    pendingHealing = 0;
    pendingMaxHealthGain = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }

  private BattleBlockOutlinePayload createBattleBlockOutlinePayload() {
    ArrayList<BattleBlockOutlinePayload.OutlinedBlock> outlinedBlocks = new ArrayList<>();
    collectOutlinedBlocks(outlinedBlocks, battleState.getRedTeam());
    collectOutlinedBlocks(outlinedBlocks, battleState.getBlueTeam());
    return new BattleBlockOutlinePayload(outlinedBlocks);
  }

  private void collectOutlinedBlocks(
      ArrayList<BattleBlockOutlinePayload.OutlinedBlock> outlinedBlocks,
      BattleTeam ownerTeam) {
    for (PlacedBattleBlock placedBlock : ownerTeam.getPlacedBlocks()) {
      if (!placedBlock.stillExists()) {
        continue;
      }

      outlinedBlocks.add(new BattleBlockOutlinePayload.OutlinedBlock(
          placedBlock.level().dimension().identifier().toString(),
          placedBlock.pos(),
          ownerTeam.getSide()));
    }
  }

  private void syncTrackedBattleBlocks(ServerPlayer player, BattleBlockOutlinePayload payload) {
    if (ServerPlayNetworking.canSend(player, BattleBlockOutlinePayload.TYPE)) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  int getPerTurnDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int damageAmount = getPerTurnDamageAmount(placedBlock.battleBlock(), currentTeam)
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.CRIMSON_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      damageAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.LAVA
        && (isWarpActive(BattleWarp.NETHER)
            || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
            || isWarpActive(BattleWarp.NETHER_FORTRESS))) {
      damageAmount *= 2;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.SPAWNER && isWarpActive(BattleWarp.NETHER_FORTRESS)) {
      damageAmount = (damageAmount * 3) / 2;
    }

    damageAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(placedBlock, damageAmount, BattleBlockIDs.RED_CARPET);
  }

  int getPerTurnDamageAmount(BattleBlock battleBlock, BattleTeam currentTeam) {
    int damageAmount = battleBlock.damage;

    if (battleBlock.id == BattleBlockIDs.DEAD_BUSH) {
      damageAmount += currentTeam.getMissingHealth() / 5;
    }

    return damageAmount;
  }

  private int getPerTurnHealingAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int healingAmount = placedBlock.battleBlock().healing
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.WARPED_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      healingAmount += 3;
    }

    healingAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(placedBlock, healingAmount, BattleBlockIDs.GREEN_CARPET);
  }

  private int getPerTurnShieldAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int shieldAmount = placedBlock.battleBlock().defence + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.END_STONE && isWarpActive(BattleWarp.BED_WARS)) {
      shieldAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.SPAWNER && isWarpActive(BattleWarp.NETHER_FORTRESS)) {
      shieldAmount = (shieldAmount * 3) / 2;
    }

    shieldAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(
        placedBlock,
        shieldAmount,
        BattleBlockIDs.BLUE_CARPET);
  }

  private int getPerTurnShieldDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int shieldDamageAmount = placedBlock.battleBlock().defenceDamage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
    return shieldDamageAmount * getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());
  }

  private int applyCarpetModifier(PlacedBattleBlock placedBlock, int amount, BattleBlockIDs carpetId) {
    if (amount == 0) {
      return 0;
    }

    BlockPos abovePos = placedBlock.pos().above();
    String blockId = BuiltInRegistries.BLOCK.getKey(placedBlock.level().getBlockState(abovePos).getBlock()).toString();
    return blockId.equals(carpetId.getId()) ? amount * 2 : amount;
  }

  public boolean canBreakBattleBlock(ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return true;
    }

    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock == null) {
      return true;
    }

    if (areAllBlocksBreakable()) {
      return true;
    }

    if (isWarpActive(BattleWarp.NETHER_FORTRESS) && trackedBlock.placedBlock().battleBlock().id == BattleBlockIDs.NETHER_BRICKS) {
      return false;
    }

    return !isUnbreakableBattleBlock(trackedBlock.placedBlock().battleBlock().id);
  }

  public void onBattleBlockBroken(ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return;
    }

    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock == null) {
      return;
    }

    trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
    handleBrokenPlacedBlock(
        trackedBlock.ownerTeam(),
        battleState.getOpponentOf(trackedBlock.ownerTeam().getSide()),
        trackedBlock.placedBlock());
  }

  private void breakBlocksAround(ServerLevel level, BlockPos pos) {
    breakBlocksInPositions(level, java.util.List.of(
        pos.north(),
        pos.south(),
        pos.east(),
        pos.west(),
        pos));
  }

  private void breakBattleBlockAt(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock != null && isUnbreakableBattleBlock(trackedBlock.placedBlock().battleBlock().id)) {
      return;
    }

    if (trackedBlock != null) {
      trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
      handleBrokenPlacedBlock(
          trackedBlock.ownerTeam(),
          battleState.getOpponentOf(trackedBlock.ownerTeam().getSide()),
          trackedBlock.placedBlock());
    }

    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    onAnyBlockBroken(level, pos);
  }

  private void handleBrokenPlacedBlock(BattleTeam ownerTeam, BattleTeam enemyTeam, PlacedBattleBlock placedBlock) {
    switch (placedBlock.battleBlock().id) {
      case NETHER_QUARTZ_ORE -> dealImmediateDamage(ownerTeam, enemyTeam, 7);
      case ANCIENT_DEBRIS -> dealImmediateDamage(ownerTeam, enemyTeam, 50);
      default -> {
      }
    }

    if (isGoldenBlock(placedBlock.battleBlock().id)) {
      triggerPiglinHeadBrokenGoldAbility(placedBlock.level(), placedBlock.pos());
    }
  }

  private void triggerPiglinHeadBrokenGoldAbility(ServerLevel level, BlockPos brokenBlockPos) {
    for (BattleTeam team : java.util.List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();

      while (iterator.hasNext()) {
        PlacedBattleBlock placedBlock = iterator.next();

        if (!refreshPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock)) {
          iterator.remove();
          continue;
        }

        if (placedBlock.battleBlock().id != BattleBlockIDs.PIGLIN_HEAD) {
          continue;
        }

        if (placedBlock.level() != level) {
          continue;
        }

        if (!areAdjacent(placedBlock.pos(), brokenBlockPos)) {
          continue;
        }

        BattleTeam enemyTeam = battleState.getOpponentOf(team.getSide());
        applyImmediateShieldGain(team, 4);
        applyImmediateHealing(team, enemyTeam, 10);
        dealImmediateDamage(team, enemyTeam, 10);
      }
    }
  }

  private boolean areAdjacent(BlockPos firstPos, BlockPos secondPos) {
    return firstPos.distManhattan(secondPos) == 1;
  }

  private boolean isWithinCube(BlockPos firstPos, BlockPos secondPos, int radius) {
    return Math.abs(firstPos.getX() - secondPos.getX()) <= radius
        && Math.abs(firstPos.getY() - secondPos.getY()) <= radius
        && Math.abs(firstPos.getZ() - secondPos.getZ()) <= radius;
  }

  private boolean isUnbreakableBattleBlock(BattleBlockIDs battleBlockId) {
    return battleBlockId == BattleBlockIDs.ANCIENT_DEBRIS || battleBlockId == BattleBlockIDs.NETHERITE_BLOCK;
  }

  private void dealImmediateDamage(BattleTeam attackingTeam, BattleTeam defendingTeam, int amount) {
    int adjustedAmount = adjustDamageAmount(amount);
    int actualDamageDealt = shouldIgnoreDefenceForDamage()
        ? defendingTeam.takeDirectHealthDamage(adjustedAmount)
        : defendingTeam.takeHealthDamage(adjustedAmount);

    tryReviveWithBed(defendingTeam);
    attackingTeam.recordLastDamageDealt(actualDamageDealt);
    defendingTeam.recordLastDamageTakenFromOpponent(actualDamageDealt);
    applyNetherGoldOreRetaliation(defendingTeam, actualDamageDealt);
    applyCactusReflection(defendingTeam, attackingTeam, actualDamageDealt);
  }

  private void applyNetherGoldOreRetaliation(BattleTeam damagedTeam, int actualDamageDealt) {
    if (actualDamageDealt <= 0) {
      return;
    }

    applyImmediateShieldGain(damagedTeam, countBlocksOnBoard(damagedTeam, BattleBlockIDs.NETHER_GOLD_ORE));
  }

  private int countBlocksOnBoard(BattleTeam team, BattleBlockIDs battleBlockId) {
    int count = 0;
    Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!refreshPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock)) {
        iterator.remove();
        continue;
      }

      if (placedBlock.battleBlock().id == battleBlockId) {
        count++;
      }
    }

    return count;
  }

  public void onAnyBlockPlaced(ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return;
    }

    triggerCalibratedSculkSensors(level, pos);
    triggerSculkShriekers(level, pos);
    reevaluateActiveWarp(level, pos);
    applyActiveWarpBoardEffectAt(level, pos);
  }

  public void onAnyBlockBroken(ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return;
    }

    triggerSculkSensors(level, pos);
    triggerSculkCatalysts(level, pos);
    triggerSculkShriekers(level, pos);
    reevaluateActiveWarp(level, pos);
  }

  private void triggerSculkSensors(ServerLevel level, BlockPos pos) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 2, true)) {
      if (trackedBlock.placedBlock().battleBlock().id == BattleBlockIDs.SCULK_SENSOR) {
        dealImmediateDamage(trackedBlock.ownerTeam(), battleState.getOpponentOf(trackedBlock.ownerTeam().getSide()), 8);
      }
    }
  }

  private void triggerCalibratedSculkSensors(ServerLevel level, BlockPos pos) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 2, true)) {
      if (trackedBlock.placedBlock().battleBlock().id == BattleBlockIDs.CALIBRATED_SCULK_SENSOR) {
        dealImmediateDamage(trackedBlock.ownerTeam(), battleState.getOpponentOf(trackedBlock.ownerTeam().getSide()), 5);
      }
    }
  }

  private void triggerSculkShriekers(ServerLevel level, BlockPos pos) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 1, true)) {
      if (trackedBlock.placedBlock().battleBlock().id != BattleBlockIDs.SCULK_SHRIEKER) {
        continue;
      }

      activateAllSculkSensors(trackedBlock.ownerTeam());
    }
  }

  private void activateAllSculkSensors(BattleTeam ownerTeam) {
    for (PlacedBattleBlock placedBlock : new ArrayList<>(ownerTeam.getPlacedBlocks())) {
      if (!placedBlock.stillExists()) {
        continue;
      }

      switch (placedBlock.battleBlock().id) {
        case SCULK_SENSOR -> dealImmediateDamage(ownerTeam, battleState.getOpponentOf(ownerTeam.getSide()), 8);
        case CALIBRATED_SCULK_SENSOR -> dealImmediateDamage(ownerTeam, battleState.getOpponentOf(ownerTeam.getSide()), 5);
        default -> {
        }
      }
    }
  }

  private void triggerSculkCatalysts(ServerLevel level, BlockPos pos) {
    if (!level.getBlockState(pos).isAir()) {
      return;
    }

    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 2, true)) {
      if (trackedBlock.placedBlock().battleBlock().id != BattleBlockIDs.SCULK_CATALYST) {
        continue;
      }

      level.setBlock(pos, Blocks.SCULK.defaultBlockState(), 3);
      trackedBlock.ownerTeam().addPlacedBlock(new PlacedBattleBlock(level, pos, CreateBlocks.SCULK));
      break;
    }
  }

  private int countNearbyOwnedBlocks(BattleTeam team, ServerLevel level, BlockPos centerPos, int radius, BattleBlockIDs targetBlockId) {
    int count = 0;

    for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
      if (placedBlock.level() != level || !placedBlock.stillExists() || placedBlock.pos().equals(centerPos)) {
        continue;
      }

      if (placedBlock.battleBlock().id == targetBlockId && isWithinCube(placedBlock.pos(), centerPos, radius)) {
        count++;
      }
    }

    return count;
  }

  private boolean hasNearbyBlock(ServerLevel level, BlockPos centerPos, int radius, BattleBlockIDs targetBlockId) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, centerPos, radius, false)) {
      if (trackedBlock.placedBlock().battleBlock().id == targetBlockId) {
        return true;
      }
    }

    return false;
  }

  private ArrayList<OwnedPlacedBattleBlock> getTrackedBlocksInCube(
      ServerLevel level,
      BlockPos centerPos,
      int radius,
      boolean includeCenter) {
    ArrayList<OwnedPlacedBattleBlock> trackedBlocks = new ArrayList<>();
    collectTrackedBlocksInCube(trackedBlocks, battleState.getRedTeam(), level, centerPos, radius, includeCenter);
    collectTrackedBlocksInCube(trackedBlocks, battleState.getBlueTeam(), level, centerPos, radius, includeCenter);
    return trackedBlocks;
  }

  private void collectTrackedBlocksInCube(
      ArrayList<OwnedPlacedBattleBlock> trackedBlocks,
      BattleTeam ownerTeam,
      ServerLevel level,
      BlockPos centerPos,
      int radius,
      boolean includeCenter) {
    for (PlacedBattleBlock placedBlock : ownerTeam.getPlacedBlocks()) {
      if (placedBlock.level() != level || !placedBlock.stillExists()) {
        continue;
      }

      if (!includeCenter && placedBlock.pos().equals(centerPos)) {
        continue;
      }

      if (isWithinCube(placedBlock.pos(), centerPos, radius)) {
        trackedBlocks.add(new OwnedPlacedBattleBlock(ownerTeam, placedBlock));
      }
    }
  }

  private boolean refreshPlacedBlock(BattleTeam ownerTeam, BattleTeam enemyTeam, PlacedBattleBlock placedBlock) {
    if (placedBlock.stillExists()) {
      return true;
    }

    BlockPos fallenPos = findFallenBlockPosition(placedBlock);

    if (fallenPos != null) {
      int blocksFallen = placedBlock.pos().getY() - fallenPos.getY();
      placedBlock.moveTo(fallenPos);

      if (blocksFallen > 0) {
        handleFallenPlacedBlock(ownerTeam, enemyTeam, placedBlock, blocksFallen);
      }

      return true;
    }

    handleBrokenPlacedBlock(ownerTeam, enemyTeam, placedBlock);
    return false;
  }

  private BlockPos findFallenBlockPosition(PlacedBattleBlock placedBlock) {
    if (!canRelocateFallingBlock(placedBlock.battleBlock().id)) {
      return null;
    }

    for (int y = placedBlock.pos().getY() - 1; y >= placedBlock.level().getMinY(); y--) {
      BlockPos candidatePos = new BlockPos(placedBlock.pos().getX(), y, placedBlock.pos().getZ());
      String blockId = BuiltInRegistries.BLOCK.getKey(placedBlock.level().getBlockState(candidatePos).getBlock()).toString();

      if (blockId.equals(placedBlock.battleBlock().id.getId())) {
        return candidatePos;
      }
    }

    return null;
  }

  private boolean canRelocateFallingBlock(BattleBlockIDs battleBlockId) {
    return battleBlockId == BattleBlockIDs.ANVIL || battleBlockId == BattleBlockIDs.DAMAGED_ANVIL;
  }

  private void handleFallenPlacedBlock(
      BattleTeam ownerTeam,
      BattleTeam enemyTeam,
      PlacedBattleBlock placedBlock,
      int blocksFallen) {
    switch (placedBlock.battleBlock().id) {
      case ANVIL -> dealImmediateDamage(ownerTeam, enemyTeam, blocksFallen * 6);
      case DAMAGED_ANVIL -> {
        dealImmediateDamage(ownerTeam, enemyTeam, blocksFallen * 10);
        dealDamageToTeam(ownerTeam, blocksFallen * 5);
      }
      default -> {
      }
    }
  }

  private int countNearbyBlocks(ServerLevel level, BlockPos centerPos, int radius) {
    int count = 0;

    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      if (targetPos.equals(centerPos)) {
        continue;
      }

      if (!level.getBlockState(targetPos).isAir()) {
        count++;
      }
    }

    return count;
  }

  private void replaceNearbyBlocksWithGrass(ServerLevel level, BlockPos centerPos, int radius) {
    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      if (targetPos.equals(centerPos) || level.getBlockState(targetPos).isAir()) {
        continue;
      }

      removeTrackedBlockSilently(level, targetPos.immutable());
      level.setBlock(targetPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
    }
  }

  private int replaceNearbyGrassWithSoulSand(ServerLevel level, BlockPos centerPos, int radius) {
    int replacedBlockCount = 0;

    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock()).toString();

      if (!blockId.equals(BattleBlockIDs.GRASS_BLOCK.getId())) {
        continue;
      }

      removeTrackedBlockSilently(level, targetPos.immutable());
      level.setBlock(targetPos, Blocks.SOUL_SAND.defaultBlockState(), 3);
      replacedBlockCount++;
    }

    return replacedBlockCount;
  }

  private void removeTrackedBlockSilently(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock != null) {
      trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
    }
  }

  private void breakBlocksInCube(ServerLevel level, BlockPos centerPos, int radius, boolean includeCenter) {
    ArrayList<BlockPos> positionsToBreak = new ArrayList<>();

    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      if (!includeCenter && targetPos.equals(centerPos)) {
        continue;
      }

      positionsToBreak.add(targetPos.immutable());
    }

    breakBlocksInPositions(level, positionsToBreak);
  }

  private void breakBlocksInPositions(ServerLevel level, Iterable<BlockPos> positions) {
    for (BlockPos targetPos : positions) {
      breakBattleBlockAt(level, targetPos);
    }
  }

  private void applyCactusReflection(BattleTeam damagedTeam, BattleTeam attackingTeam, int actualDamageDealt) {
    int cactusCount = countBlocksOnBoard(damagedTeam, BattleBlockIDs.CACTUS);

    if (actualDamageDealt <= 0 || cactusCount <= 0) {
      return;
    }

    int reflectedDamage = (actualDamageDealt / 2) * cactusCount;
    int actualReflectedDamage = shouldIgnoreDefenceForDamage()
        ? attackingTeam.takeDirectHealthDamage(adjustDamageAmount(reflectedDamage))
        : attackingTeam.takeHealthDamage(adjustDamageAmount(reflectedDamage));

    tryReviveWithBed(attackingTeam);
    damagedTeam.recordLastDamageDealt(actualReflectedDamage);
    applyNetherGoldOreRetaliation(attackingTeam, actualReflectedDamage);
  }

  private boolean isGoldenBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case GOLD_BLOCK, RAW_GOLD_BLOCK, NETHER_GOLD_ORE, DEEPSLATE_GOLD_ORE -> true;
      default -> false;
    };
  }

  private OwnedPlacedBattleBlock findTrackedBlock(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(battleState.getRedTeam(), level, pos);

    if (trackedBlock != null) {
      return trackedBlock;
    }

    return findTrackedBlock(battleState.getBlueTeam(), level, pos);
  }

  private OwnedPlacedBattleBlock findTrackedBlock(BattleTeam team, ServerLevel level, BlockPos pos) {
    for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
      if (placedBlock.level() == level && placedBlock.pos().equals(pos)) {
        return new OwnedPlacedBattleBlock(team, placedBlock);
      }
    }

    return null;
  }

  private boolean isWarpActive(BattleWarp warp) {
    return battleState.getActiveWarp() == warp;
  }

  private boolean shouldIgnoreDefenceForDamage() {
    return isWarpActive(BattleWarp.SOUL_SAND_VALLEY);
  }

  private boolean arePerTurnBlocksDisabled() {
    return isWarpActive(BattleWarp.SOUL_SAND_VALLEY);
  }

  private boolean areExplosionsDisabled() {
    return isWarpActive(BattleWarp.OCEAN);
  }

  private boolean doBedsExplodeOnPlacement() {
    return isWarpActive(BattleWarp.END)
        || isWarpActive(BattleWarp.NETHER)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_FORTRESS);
  }

  private boolean areAllBlocksBreakable() {
    return isWarpActive(BattleWarp.STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.DRIPSTONE_CAVE)
        || isWarpActive(BattleWarp.CAVE);
  }

  private boolean isPlacementBlockedByWarp(String blockId) {
    boolean blockedByNetherWarp = isWarpActive(BattleWarp.NETHER)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_FORTRESS);

    if (!blockedByNetherWarp) {
      return false;
    }

    return blockId.equals(BattleBlockIDs.WATER.getId()) || blockId.equals(BattleBlockIDs.POWDER_SNOW.getId());
  }

  private boolean isCoralSuppressedByWarp(BattleBlockIDs battleBlockId) {
    if (!(isWarpActive(BattleWarp.NETHER)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_FORTRESS))) {
      return false;
    }

    return switch (battleBlockId) {
      case HORN_CORAL_BLOCK,
          TUBE_CORAL_BLOCK,
          BUBBLE_CORAL_BLOCK,
          FIRE_CORAL_BLOCK,
          BUBBLE_CORAL_FAN,
          HORN_CORAL_FAN,
          TUBE_CORAL_FAN -> true;
      default -> false;
    };
  }

  private int getWarpDrawModifier() {
    return switch (battleState.getActiveWarp()) {
      case STRIP_MINE, NETHER_STRIP_MINE -> 2;
      default -> 0;
    };
  }

  private int getPlacementsPerTurnForNewTurn() {
    if (isWarpActive(BattleWarp.BLIZZARD)) {
      return Math.max(1, battleState.getActiveWarpTurnCount() + 1);
    }

    return 1;
  }

  private int getBaseHandSizeForNewTurn() {
    if (isWarpActive(BattleWarp.BLIZZARD)) {
      return Math.max(1, battleState.getActiveWarpTurnCount() + 1);
    }

    return BattleCardItems.HAND_SIZE;
  }

  private int getBaseHandSizeForCurrentTurn(BattleTeam team) {
    if (isWarpActive(BattleWarp.BLIZZARD) && team == battleState.getActiveTeam()) {
      return Math.max(1, battleState.getRemainingPlacementsThisTurn());
    }

    return BattleCardItems.HAND_SIZE;
  }

  private int adjustDamageAmount(int amount) {
    if (isWarpActive(BattleWarp.DRIPSTONE_CAVE)) {
      return amount * 2;
    }

    return amount;
  }

  private int adjustHealingAmount(int amount) {
    if (isWarpActive(BattleWarp.PALE_GARDEN)) {
      return -amount;
    }

    return amount;
  }

  private int adjustShieldGainAmount(int amount) {
    if (isWarpActive(BattleWarp.MESA)) {
      return amount * 2;
    }

    return amount;
  }

  private int adjustShieldDamageAmount(int amount) {
    int adjustedAmount = amount;

    if (isWarpActive(BattleWarp.DRIPSTONE_CAVE)) {
      adjustedAmount *= 2;
    }

    if (isWarpActive(BattleWarp.BASTION) || isWarpActive(BattleWarp.MESA)) {
      adjustedAmount *= 2;
    }

    if (isWarpActive(BattleWarp.CAVE)) {
      adjustedAmount /= 2;
    }

    return adjustedAmount;
  }

  private int getBlockEffectMultiplier(ServerLevel level, BlockPos pos, BattleBlock battleBlock) {
    if (!isWarpActive(BattleWarp.BASTION) || level == null || pos == null || battleBlock == null) {
      return 1;
    }

    return hasAdjacentGoldenBlock(level, pos) ? 2 : 1;
  }

  private boolean hasAdjacentGoldenBlock(ServerLevel level, BlockPos pos) {
    for (Direction direction : Direction.values()) {
      if (isGoldenBlockId(getBlockId(level, pos.relative(direction)))) {
        return true;
      }
    }

    return false;
  }

  private void applyImmediateHealing(BattleTeam team, BattleTeam enemyTeam, int amount) {
    int adjustedAmount = adjustHealingAmount(amount);
    team.heal(adjustedAmount);

    if (isWarpActive(BattleWarp.LUSH_CAVE) && adjustedAmount > 0) {
      dealImmediateDamage(team, enemyTeam, adjustedAmount);
    }
  }

  private void applyImmediateShieldGain(BattleTeam team, int amount) {
    team.gainShield(adjustShieldGainAmount(amount));
  }

  private int dealDamageToTeam(BattleTeam team, int amount) {
    int adjustedAmount = adjustDamageAmount(amount);
    int actualDamageDealt = shouldIgnoreDefenceForDamage()
        ? team.takeDirectHealthDamage(adjustedAmount)
        : team.takeHealthDamage(adjustedAmount);

    tryReviveWithBed(team);
    return actualDamageDealt;
  }

  private void applyStartOfTurnWarpCosts(BattleTeam currentTeam, BattleTeam enemyTeam) {
    if (isWarpActive(BattleWarp.VILLAGE_HOUSE)) {
      applyImmediateHealing(currentTeam, enemyTeam, 16);
      applyImmediateHealing(enemyTeam, currentTeam, 16);
    }

    if (isWarpActive(BattleWarp.DEEP_DARK) && currentTeam.removeRandomCardFromDeck(random) == null) {
      dealDamageToTeam(currentTeam, 30);
    }

    if (isWarpActive(BattleWarp.DESERT) && currentTeam.getShield() < 2) {
      dealDamageToTeam(currentTeam, 20);
    }

    if (isWarpActive(BattleWarp.SWAMP)) {
      dealDamageToTeam(currentTeam, 3);
    }
  }

  private void applyEndOfTurnWarpCosts(BattleTeam currentTeam, BattleTeam enemyTeam) {
  }

  private void tryReviveWithBed(BattleTeam team) {
    if (!isWarpActive(BattleWarp.BED_WARS) || team.getHealth() > 0) {
      return;
    }

    for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
      if (placedBlock.battleBlock().id != BattleBlockIDs.RED_BED || !placedBlock.stillExists()) {
        continue;
      }

      team.getPlacedBlocks().remove(placedBlock);
      placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
      team.setHealth(10);
      onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
      return;
    }
  }

  private void reevaluateActiveWarp(ServerLevel level, BlockPos changedPos) {
    WarpMatch matchingWarp = findMatchingWarp(level, changedPos);
    BattleWarp newWarp = matchingWarp == null ? BattleWarp.NONE : matchingWarp.warp();

    if (newWarp == battleState.getActiveWarp()) {
      return;
    }

    BattleWarp previousWarp = battleState.getActiveWarp();
    TeamSide starterSide = matchingWarp == null
        ? null
        : resolveWarpStarterSide(level, matchingWarp.anchorPos(), changedPos);

    battleState.setActiveWarp(newWarp);
    battleState.setActiveWarpStarterSide(starterSide);
    battleState.resetActiveWarpTurnCount();
    battleState.setActiveWarpRoundCount(newWarp == BattleWarp.END && starterSide != null ? 1 : 0);

    if (previousWarp == BattleWarp.FLOWER_FOREST) {
      removeFlowerForestBonus(battleState.getRedTeam());
      removeFlowerForestBonus(battleState.getBlueTeam());
    }

    if (newWarp == BattleWarp.FLOWER_FOREST) {
      applyFlowerForestBonus(battleState.getRedTeam());
      applyFlowerForestBonus(battleState.getBlueTeam());
    }

    applyBoardWideWarpEffects(level);

    if (matchingWarp != null) {
      placeWarpStructure(level, matchingWarp.anchorPos(), matchingWarp.warp());
    }
  }

  private WarpMatch findMatchingWarp(ServerLevel level, BlockPos changedPos) {
    ArrayList<BlockPos> candidatePositions = collectWarpCandidatePositions(level, changedPos);

    for (BattleWarp warp : BattleWarp.values()) {
      if (warp == BattleWarp.NONE || warp == BattleWarp.SWAMP) {
        continue;
      }

      for (BlockPos candidatePos : candidatePositions) {
        if (matchesWarpAt(level, candidatePos, warp)) {
          return new WarpMatch(warp, candidatePos);
        }
      }
    }

    return null;
  }

  private ArrayList<BlockPos> collectWarpCandidatePositions(ServerLevel level, BlockPos changedPos) {
    ArrayList<BlockPos> candidatePositions = new ArrayList<>();
    Set<BlockPos> seenPositions = new HashSet<>();

    if (changedPos != null) {
      BlockPos immutablePos = changedPos.immutable();
      candidatePositions.add(immutablePos);
      seenPositions.add(immutablePos);
    }

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
        if (placedBlock.level() != level || !placedBlock.stillExists()) {
          continue;
        }

        BlockPos immutablePos = placedBlock.pos().immutable();

        if (seenPositions.add(immutablePos)) {
          candidatePositions.add(immutablePos);
        }
      }
    }

    return candidatePositions;
  }

  private TeamSide resolveWarpStarterSide(ServerLevel level, BlockPos anchorPos, BlockPos changedPos) {
    TeamSide starterSide = getTrackedOwnerSide(level, changedPos);

    if (starterSide != null) {
      return starterSide;
    }

    starterSide = getTrackedOwnerSide(level, anchorPos);

    if (starterSide != null) {
      return starterSide;
    }

    for (Direction direction : Direction.values()) {
      starterSide = getTrackedOwnerSide(level, anchorPos.relative(direction));

      if (starterSide != null) {
        return starterSide;
      }
    }

    return battleState.getActiveSide();
  }

  private TeamSide getTrackedOwnerSide(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return null;
    }

    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);
    return trackedBlock == null ? null : trackedBlock.ownerTeam().getSide();
  }

  private boolean matchesWarpAt(ServerLevel level, BlockPos pos, BattleWarp warp) {
    return switch (warp) {
      case VILLAGE_HOUSE -> matchesVillageHouseWarp(level, pos);
      case END -> matchesEndWarp(level, pos);
      case LIBRARY -> matchesLibraryWarp(level, pos);
      case DEEP_DARK -> matchesDeepDarkWarp(level, pos);
      case SOUL_SAND_VALLEY -> matchesSoulSandValleyWarp(level, pos);
      case BLIZZARD -> matchesBlizzardWarp(level, pos);
      case OCEAN -> matchesOceanWarp(level, pos);
      case REDSTONE -> matchesRedstoneWarp(level, pos);
      case NETHER_STRIP_MINE -> matchesNetherStripMineWarp(level, pos);
      case NETHER -> matchesNetherWarp(level, pos);
      case LUSH_CAVE -> matchesLushCaveWarp(level, pos);
      case DESERT -> matchesDesertWarp(level, pos);
      case STRIP_MINE -> matchesStripMineWarp(level, pos);
      case BED_WARS -> matchesBedWarsWarp(level, pos);
      case DRIPSTONE_CAVE -> matchesDripstoneCaveWarp(level, pos);
      case NETHER_FORTRESS -> matchesNetherFortressWarp(level, pos);
      case BASTION -> matchesBastionWarp(level, pos);
      case FLOWER_FOREST -> matchesFlowerForestWarp(level, pos);
      case CAVE -> matchesCaveWarp(level, pos);
      case MESA -> matchesMesaWarp(level, pos);
      case PALE_GARDEN -> matchesPaleGardenWarp(level, pos);
      case NONE, SWAMP -> false;
    };
  }

  private boolean matchesVillageHouseWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.RED_BED)) {
      return hasHorizontalAdjacent(
          level,
          pos,
          BattleBlockIDs.RED_CARPET,
          BattleBlockIDs.BLUE_CARPET,
          BattleBlockIDs.GREEN_CARPET,
          BattleBlockIDs.PALE_MOSS_CARPET,
          BattleBlockIDs.CAKE,
          BattleBlockIDs.JUNGLE_LOG,
          BattleBlockIDs.BOOKSHELF);
    }

    return isAnyBlock(
        level,
        pos,
        BattleBlockIDs.RED_CARPET,
        BattleBlockIDs.BLUE_CARPET,
        BattleBlockIDs.GREEN_CARPET,
        BattleBlockIDs.PALE_MOSS_CARPET,
        BattleBlockIDs.CAKE,
        BattleBlockIDs.JUNGLE_LOG,
        BattleBlockIDs.BOOKSHELF)
        && hasHorizontalAdjacent(level, pos, BattleBlockIDs.RED_BED);
  }

  private boolean matchesEndWarp(ServerLevel level, BlockPos pos) {
    if (isAnyBlock(level, pos, BattleBlockIDs.DRAGON_HEAD, BattleBlockIDs.SHULKER_BOX, BattleBlockIDs.DRAGON_EGG)) {
      return isBlock(level, pos.below(), BattleBlockIDs.END_STONE);
    }

    return isBlock(level, pos, BattleBlockIDs.END_STONE)
        && isAnyBlock(level, pos.above(), BattleBlockIDs.DRAGON_HEAD, BattleBlockIDs.SHULKER_BOX, BattleBlockIDs.DRAGON_EGG);
  }

  private boolean matchesLibraryWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.CANDLE)) {
      return isAnyBlock(level, pos.below(), BattleBlockIDs.BOOKSHELF, BattleBlockIDs.CHISELED_BOOKSHELF);
    }

    if (isBlock(level, pos, BattleBlockIDs.BOOKSHELF)) {
      return hasHorizontalAdjacent(level, pos, BattleBlockIDs.CHISELED_BOOKSHELF, BattleBlockIDs.LECTERN)
          || isBlock(level, pos.above(), BattleBlockIDs.CANDLE);
    }

    if (isBlock(level, pos, BattleBlockIDs.CHISELED_BOOKSHELF)) {
      return hasHorizontalAdjacent(level, pos, BattleBlockIDs.BOOKSHELF, BattleBlockIDs.LECTERN)
          || isBlock(level, pos.above(), BattleBlockIDs.CANDLE);
    }

    return isBlock(level, pos, BattleBlockIDs.LECTERN)
        && hasHorizontalAdjacent(level, pos, BattleBlockIDs.BOOKSHELF, BattleBlockIDs.CHISELED_BOOKSHELF);
  }

  private boolean matchesDeepDarkWarp(ServerLevel level, BlockPos pos) {
    BattleBlockIDs centerBlockId = getBattleBlockId(level, pos);

    if (centerBlockId == null || !isDeepDarkWarpBlock(centerBlockId)) {
      return false;
    }

    for (Direction direction : Direction.values()) {
      BattleBlockIDs adjacentBlockId = getBattleBlockId(level, pos.relative(direction));

      if (adjacentBlockId != null && adjacentBlockId != centerBlockId && isDeepDarkWarpBlock(adjacentBlockId)) {
        return true;
      }
    }

    return false;
  }

  private boolean matchesSoulSandValleyWarp(ServerLevel level, BlockPos pos) {
    return isAnyBlock(level, pos, BattleBlockIDs.SOUL_CAMPFIRE, BattleBlockIDs.SOUL_LANTERN, BattleBlockIDs.SOUL_TORCH)
        && isBlock(level, pos.below(), BattleBlockIDs.SOUL_SAND);
  }

  private boolean matchesBlizzardWarp(ServerLevel level, BlockPos pos) {
    if (isAnyBlock(level, pos, BattleBlockIDs.SNOW, BattleBlockIDs.POWDER_SNOW)) {
      return hasAdjacent(level, pos, BattleBlockIDs.SNOW, BattleBlockIDs.POWDER_SNOW)
          || isBlock(level, pos.above(), BattleBlockIDs.LIGHTNING_ROD);
    }

    return isBlock(level, pos, BattleBlockIDs.LIGHTNING_ROD)
        && isAnyBlock(level, pos.below(), BattleBlockIDs.SNOW, BattleBlockIDs.POWDER_SNOW);
  }

  private boolean matchesOceanWarp(ServerLevel level, BlockPos pos) {
    if (isCoralBlock(level, pos) && hasAdjacent(level, pos, BattleBlockIDs.WATER) && hasAdjacent(level, pos, BattleBlockIDs.PRISMARINE)) {
      return true;
    }

    if (isBlock(level, pos, BattleBlockIDs.WATER)) {
      if (hasAdjacent(level, pos, BattleBlockIDs.PRISMARINE)) {
        return true;
      }

      if (countAdjacent(level, pos, BattleBlockIDs.WATER) >= 2) {
        return true;
      }

      return hasAdjacentCoralBlock(level, pos);
    }

    return isBlock(level, pos, BattleBlockIDs.PRISMARINE) && hasAdjacent(level, pos, BattleBlockIDs.WATER);
  }

  private boolean matchesRedstoneWarp(ServerLevel level, BlockPos pos) {
    return isAnyBlock(level, pos, BattleBlockIDs.REDSTONE_TORCH, BattleBlockIDs.REPEATER)
        && isBlock(level, pos.below(), BattleBlockIDs.REDSTONE_BLOCK);
  }

  private boolean matchesNetherStripMineWarp(ServerLevel level, BlockPos pos) {
    BattleBlockIDs centerBlockId = getBattleBlockId(level, pos);

    if (centerBlockId == null || !isAnyNetherStripMineBlock(centerBlockId)) {
      return false;
    }

    for (Direction direction : Direction.values()) {
      BattleBlockIDs adjacentBlockId = getBattleBlockId(level, pos.relative(direction));

      if (adjacentBlockId != null && adjacentBlockId != centerBlockId && isAnyNetherStripMineBlock(adjacentBlockId)) {
        return true;
      }
    }

    return false;
  }

  private boolean matchesNetherWarp(ServerLevel level, BlockPos pos) {
    return isAnyBlock(level, pos, BattleBlockIDs.LAVA, BattleBlockIDs.MAGMA_BLOCK, BattleBlockIDs.CAMPFIRE)
        && isAnyBlock(level, pos.below(), BattleBlockIDs.OBSIDIAN, BattleBlockIDs.NETHERRACK);
  }

  private boolean matchesLushCaveWarp(ServerLevel level, BlockPos pos) {
    return isAnyBlock(level, pos, BattleBlockIDs.MOSS_BLOCK, BattleBlockIDs.FARMLAND)
        && isAnyBlock(level, pos.below(), BattleBlockIDs.DEEPSLATE, BattleBlockIDs.DEEPSLATE_GOLD_ORE, BattleBlockIDs.DEEPSLATE_REDSTONE_ORE);
  }

  private boolean matchesDesertWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.DEAD_BUSH) && isBlock(level, pos.below(), BattleBlockIDs.SAND)) {
      return true;
    }

    if (isBlock(level, pos, BattleBlockIDs.CACTUS)
        && isBlock(level, pos.below(), BattleBlockIDs.SAND)
        && hasAdjacent(level, pos.below(), BattleBlockIDs.SAND)) {
      return true;
    }

    return isBlock(level, pos, BattleBlockIDs.SAND)
        && hasAdjacent(level, pos, BattleBlockIDs.SANDSTONE, BattleBlockIDs.SMOOTH_SANDSTONE, BattleBlockIDs.CUT_SANDSTONE);
  }

  private boolean matchesStripMineWarp(ServerLevel level, BlockPos pos) {
    BattleBlockIDs centerBlockId = getBattleBlockId(level, pos);

    if (centerBlockId == null || !isAnyStripMineBlock(centerBlockId)) {
      return false;
    }

    for (Direction direction : Direction.values()) {
      BattleBlockIDs adjacentBlockId = getBattleBlockId(level, pos.relative(direction));

      if (adjacentBlockId != null
          && isAnyStripMineBlock(adjacentBlockId)
          && (centerBlockId != BattleBlockIDs.DEEPSLATE || adjacentBlockId != BattleBlockIDs.DEEPSLATE)) {
        return true;
      }
    }

    return false;
  }

  private boolean matchesBedWarsWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.RED_BED)) {
      return countHorizontalRingBlocks(level, pos, BattleBlockIDs.OAK_PLANKS) >= 8
          || countAdjacentAndAbove(level, pos, BattleBlockIDs.END_STONE) >= 2;
    }

    return isBlock(level, pos, BattleBlockIDs.END_STONE)
        && countAdjacent(level, pos, BattleBlockIDs.OAK_PLANKS) >= 4
        && hasAdjacent(level, pos, BattleBlockIDs.END_STONE);
  }

  private boolean matchesDripstoneCaveWarp(ServerLevel level, BlockPos pos) {
    return isBlock(level, pos, BattleBlockIDs.POINTED_DRIPSTONE)
        && hasAdjacent(level, pos, BattleBlockIDs.DEEPSLATE, BattleBlockIDs.DEEPSLATE_GOLD_ORE, BattleBlockIDs.DEEPSLATE_REDSTONE_ORE);
  }

  private boolean matchesNetherFortressWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.SPAWNER) && isBlock(level, pos.below(), BattleBlockIDs.NETHER_BRICKS)) {
      return true;
    }

    return isBlock(level, pos, BattleBlockIDs.LAVA) && hasAdjacent(level, pos, BattleBlockIDs.NETHER_BRICKS);
  }

  private boolean matchesBastionWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.PIGLIN_HEAD)
        && isAnyBlock(level, pos.below(), BattleBlockIDs.SPAWNER, BattleBlockIDs.POLISHED_BLACKSTONE_BRICKS)) {
      return true;
    }

    if (isBlock(level, pos, BattleBlockIDs.GOLD_BLOCK) && isBlock(level, pos.below(), BattleBlockIDs.POLISHED_BLACKSTONE_BRICKS)) {
      return true;
    }

    return isBlock(level, pos, BattleBlockIDs.POLISHED_BLACKSTONE_BRICKS) && hasAdjacent(level, pos, BattleBlockIDs.NETHER_GOLD_ORE);
  }

  private boolean matchesFlowerForestWarp(ServerLevel level, BlockPos pos) {
    if (isFlowerBlock(level, pos) && isBlock(level, pos.below(), BattleBlockIDs.GRASS_BLOCK)) {
      return hasAdjacent(level, pos.below(), BattleBlockIDs.GRASS_BLOCK);
    }

    return isBlock(level, pos, BattleBlockIDs.GRASS_BLOCK)
        && isFlowerBlock(level, pos.above())
        && hasAdjacent(level, pos, BattleBlockIDs.GRASS_BLOCK);
  }

  private boolean matchesCaveWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.TORCH) && isBlock(level, pos.below(), BattleBlockIDs.DEEPSLATE)) {
      return true;
    }

    return isBlock(level, pos, BattleBlockIDs.REINFORCED_DEEPSLATE)
        && isAnyBlock(level, pos.below(), BattleBlockIDs.DEEPSLATE_GOLD_ORE, BattleBlockIDs.DEEPSLATE_REDSTONE_ORE);
  }

  private boolean matchesMesaWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.DEAD_BUSH) && isBlock(level, pos.below(), BattleBlockIDs.RED_SAND)) {
      return true;
    }

    if (isBlock(level, pos, BattleBlockIDs.CACTUS)
        && isBlock(level, pos.below(), BattleBlockIDs.RED_SAND)
        && hasAdjacent(level, pos.below(), BattleBlockIDs.RED_SAND)) {
      return true;
    }

    BattleBlockIDs centerBlockId = getBattleBlockId(level, pos);

    if (centerBlockId == null) {
      return false;
    }

    return switch (centerBlockId) {
      case RED_SAND -> hasAdjacent(level, pos, BattleBlockIDs.RED_SANDSTONE, BattleBlockIDs.SMOOTH_RED_SANDSTONE, BattleBlockIDs.CUT_RED_SANDSTONE, BattleBlockIDs.CHISELED_RED_SANDSTONE);
      case RED_SANDSTONE, SMOOTH_RED_SANDSTONE, CUT_RED_SANDSTONE, CHISELED_RED_SANDSTONE -> hasAdjacent(level, pos, BattleBlockIDs.RED_SAND, BattleBlockIDs.RED_SANDSTONE, BattleBlockIDs.SMOOTH_RED_SANDSTONE, BattleBlockIDs.CUT_RED_SANDSTONE, BattleBlockIDs.CHISELED_RED_SANDSTONE);
      default -> false;
    };
  }

  private boolean matchesPaleGardenWarp(ServerLevel level, BlockPos pos) {
    return isBlock(level, pos, BattleBlockIDs.CREAKING_HEART)
        && isBlock(level, pos.above(), BattleBlockIDs.PALE_OAK_LOG)
        && isBlock(level, pos.below(), BattleBlockIDs.PALE_OAK_LOG);
  }

  private void applyBoardWideWarpEffects(ServerLevel level) {
    if (level == null) {
      return;
    }

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
        if (placedBlock.level() != level || !placedBlock.stillExists()) {
          continue;
        }

        applyActiveWarpBoardEffectAt(level, placedBlock.pos());
      }
    }
  }

  private void applyActiveWarpBoardEffectAt(ServerLevel level, BlockPos pos) {
    String blockId = getBlockId(level, pos);

    if (isWarpActive(BattleWarp.OCEAN)) {
      if (isFireRelatedBlockId(blockId)) {
        removeTrackedBlockSilently(level, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return;
      }

      if (blockId.equals(BattleBlockIDs.GRASS_BLOCK.getId())) {
        replaceTrackedBlock(level, pos, Blocks.FARMLAND.defaultBlockState(), CreateBlocks.FARMLAND);
        return;
      }
    }

    if (isWarpActive(BattleWarp.BLIZZARD) && blockId.equals(BattleBlockIDs.WATER.getId())) {
      removeTrackedBlockSilently(level, pos);
      level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
      return;
    }

    if ((isWarpActive(BattleWarp.NETHER)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_FORTRESS))
        && (blockId.equals(BattleBlockIDs.WATER.getId()) || blockId.equals(BattleBlockIDs.POWDER_SNOW.getId()))) {
      removeTrackedBlockSilently(level, pos);
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
  }

  private void applyFlowerForestBonus(BattleTeam team) {
    team.increaseMaxHealth(70);
    team.setHealth(team.getHealth() + 70);
  }

  private void removeFlowerForestBonus(BattleTeam team) {
    team.setHealth(team.getHealth() - 70);
    team.increaseMaxHealth(-70);
  }

  private void placeWarpStructure(ServerLevel level, BlockPos anchorPos, BattleWarp warp) {
    Identifier structureId = warp.getStructureId();

    if (level == null || anchorPos == null || structureId == null) {
      return;
    }

    try {
      var template = level.getStructureManager().get(structureId);

      if (template.isEmpty()) {
        BlockBattlesMod.LOGGER.warn("Warp structure {} was not found. Skipping placement.", structureId);
        return;
      }

      template.get().placeInWorld(
          level,
          anchorPos,
          anchorPos,
          new StructurePlaceSettings(),
          level.getRandom(),
          3);
    } catch (RuntimeException exception) {
      BlockBattlesMod.LOGGER.warn("Could not place warp structure {}.", structureId, exception);
    }
  }

  private void replaceTrackedBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState replacementState, BattleBlock replacementBattleBlock) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock != null) {
      trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
      trackedBlock.ownerTeam().addPlacedBlock(new PlacedBattleBlock(level, pos, replacementBattleBlock));
    }

    level.setBlock(pos, replacementState, 3);
  }

  private String getBlockId(ServerLevel level, BlockPos pos) {
    return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
  }

  private BattleBlockIDs getBattleBlockId(ServerLevel level, BlockPos pos) {
    return CreateBlocks.findByMinecraftId(getBlockId(level, pos))
        .map(battleBlock -> battleBlock.id)
        .orElse(null);
  }

  private boolean isBlock(ServerLevel level, BlockPos pos, BattleBlockIDs battleBlockId) {
    return getBlockId(level, pos).equals(battleBlockId.getId());
  }

  private boolean isAnyBlock(ServerLevel level, BlockPos pos, BattleBlockIDs... battleBlockIds) {
    String blockId = getBlockId(level, pos);

    for (BattleBlockIDs battleBlockId : battleBlockIds) {
      if (blockId.equals(battleBlockId.getId())) {
        return true;
      }
    }

    return false;
  }

  private boolean hasAdjacent(ServerLevel level, BlockPos pos, BattleBlockIDs... battleBlockIds) {
    for (Direction direction : Direction.values()) {
      if (isAnyBlock(level, pos.relative(direction), battleBlockIds)) {
        return true;
      }
    }

    return false;
  }

  private int countAdjacent(ServerLevel level, BlockPos pos, BattleBlockIDs... battleBlockIds) {
    int count = 0;

    for (Direction direction : Direction.values()) {
      if (isAnyBlock(level, pos.relative(direction), battleBlockIds)) {
        count++;
      }
    }

    return count;
  }

  private int countAdjacentAndAbove(ServerLevel level, BlockPos pos, BattleBlockIDs battleBlockId) {
    int count = countAdjacent(level, pos, battleBlockId);

    if (isBlock(level, pos.above(), battleBlockId)) {
      count++;
    }

    return count;
  }

  private boolean hasHorizontalAdjacent(ServerLevel level, BlockPos pos, BattleBlockIDs... battleBlockIds) {
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      if (isAnyBlock(level, pos.relative(direction), battleBlockIds)) {
        return true;
      }
    }

    return false;
  }

  private int countHorizontalRingBlocks(ServerLevel level, BlockPos pos, BattleBlockIDs battleBlockId) {
    int count = 0;

    for (int offsetX = -1; offsetX <= 1; offsetX++) {
      for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
        if (offsetX == 0 && offsetZ == 0) {
          continue;
        }

        if (isBlock(level, pos.offset(offsetX, 0, offsetZ), battleBlockId)) {
          count++;
        }
      }
    }

    return count;
  }

  private boolean hasAdjacentCoralBlock(ServerLevel level, BlockPos pos) {
    for (Direction direction : Direction.values()) {
      if (isCoralBlock(level, pos.relative(direction))) {
        return true;
      }
    }

    return false;
  }

  private boolean isCoralBlock(ServerLevel level, BlockPos pos) {
    return isAnyBlock(
        level,
        pos,
        BattleBlockIDs.HORN_CORAL_BLOCK,
        BattleBlockIDs.TUBE_CORAL_BLOCK,
        BattleBlockIDs.BUBBLE_CORAL_BLOCK,
        BattleBlockIDs.FIRE_CORAL_BLOCK);
  }

  private boolean isFlowerBlock(ServerLevel level, BlockPos pos) {
    return isAnyBlock(
        level,
        pos,
        BattleBlockIDs.RED_TULIP,
        BattleBlockIDs.CORNFLOWER,
        BattleBlockIDs.PINK_PETALS,
        BattleBlockIDs.TORCHFLOWER,
        BattleBlockIDs.WITHER_ROSE);
  }

  private boolean isDeepDarkWarpBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case SCULK_SENSOR,
          CALIBRATED_SCULK_SENSOR,
          SCULK_SHRIEKER,
          SCULK_CATALYST,
          SCULK -> true;
      default -> false;
    };
  }

  private boolean isAnyNetherStripMineBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS -> true;
      default -> false;
    };
  }

  private boolean isAnyStripMineBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case DEEPSLATE, DEEPSLATE_GOLD_ORE, DEEPSLATE_REDSTONE_ORE -> true;
      default -> false;
    };
  }

  private boolean isAnyRedSandWarpBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case RED_SAND, RED_SANDSTONE, SMOOTH_RED_SANDSTONE, CUT_RED_SANDSTONE, CHISELED_RED_SANDSTONE -> true;
      default -> false;
    };
  }

  private boolean isFireRelatedBlockId(String blockId) {
    return blockId.equals(BattleBlockIDs.LAVA.getId())
        || blockId.equals(BattleBlockIDs.MAGMA_BLOCK.getId())
        || blockId.equals(BattleBlockIDs.CAMPFIRE.getId())
        || blockId.equals(BattleBlockIDs.SOUL_CAMPFIRE.getId())
        || blockId.equals(BattleBlockIDs.TORCH.getId())
        || blockId.equals(BattleBlockIDs.SOUL_TORCH.getId())
        || blockId.equals(BattleBlockIDs.REDSTONE_TORCH.getId())
        || blockId.equals(BattleBlockIDs.COPPER_TORCH.getId())
        || blockId.equals(BattleBlockIDs.CANDLE.getId())
        || blockId.equals(BattleBlockIDs.TORCHFLOWER.getId());
  }

  private boolean isGoldenBlockId(String blockId) {
    return blockId.equals(BattleBlockIDs.GOLD_BLOCK.getId())
        || blockId.equals(BattleBlockIDs.RAW_GOLD_BLOCK.getId())
        || blockId.equals(BattleBlockIDs.NETHER_GOLD_ORE.getId())
        || blockId.equals(BattleBlockIDs.DEEPSLATE_GOLD_ORE.getId());
  }

  private record WarpMatch(BattleWarp warp, BlockPos anchorPos) {
  }

  private record OwnedPlacedBattleBlock(BattleTeam ownerTeam, PlacedBattleBlock placedBlock) {
  }
}
