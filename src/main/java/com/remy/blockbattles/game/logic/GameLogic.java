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
import com.remy.blockbattles.game.gui.BattleAbilityChoiceMenu;
import com.remy.blockbattles.game.gui.BattleCardItems;
import com.remy.blockbattles.game.gui.BattleScoreboards;
import com.remy.blockbattles.game.gui.BattleStoredBlocksMenu;
import com.remy.blockbattles.network.BattleBlockOutlinePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class GameLogic {
  private final BattleState battleState;
  private final DeckManager deckManager;
  private final Random random = new Random();
  private MinecraftServer lastKnownServer;

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

  public void rememberServer(MinecraftServer server) {
    if (server != null) {
      lastKnownServer = server;
    }
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
        .map(battleBlock -> placeBattleBlock(battleBlock, battleState.getActiveSide(), null, null, null))
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos) {
    return onPlaceBattleBlock(blockId, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos, TeamSide actingSide) {
    return onPlaceBattleBlock(blockId, level, pos, actingSide, null);
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos, TeamSide actingSide, ServerPlayer placingPlayer) {
    if (level instanceof ServerLevel serverLevel) {
      rememberServer(serverLevel.getServer());
    }

    return CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> {
          if (level instanceof ServerLevel serverLevel) {
            return placeBattleBlock(battleBlock, actingSide, serverLevel, pos, placingPlayer);
          }

          return placeBattleBlock(battleBlock, actingSide, null, null, placingPlayer);
        })
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock) {
    return placeBattleBlock(battleBlock, battleState.getActiveSide(), null, null, null);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, TeamSide actingSide) {
    return placeBattleBlock(battleBlock, actingSide, null, null, null);
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    return onPlaceBattleBlock(battleBlock, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    return placeBattleBlock(battleBlock, actingSide, level, pos, null);
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

  private PlacedBlockResolution applyOnPlaceAbility(
      BattleBlock battleBlock,
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      ServerLevel level,
      BlockPos pos,
      ServerPlayer placingPlayer) {
    if (level != null && pos != null && isActivationSuppressed(battleBlock, level, pos)) {
      return new PlacedBlockResolution(battleBlock, battleBlock, List.of());
    }

    int effectMultiplier = getBlockEffectMultiplier(level, pos, battleBlock);

    switch (battleBlock.id) {
      case SNOW -> {
        if (level != null && pos != null && isBlock(level, pos.above(), BattleBlockIDs.CARVED_PUMPKIN)) {
          applySnowPumpkinCombo(currentTeam, enemyTeam, level, pos, effectMultiplier);
        }
      }
      case TNT -> {
        if (!areExplosionsDisabled() && level != null && pos != null) {
          triggerExplosiveCombo(level, pos, 1, true, true);
          return new PlacedBlockResolution(null, battleBlock, List.of());
        }
      }
      case RED_TULIP -> {
        for (int i = 0; i < effectMultiplier; i++) {
          changeMaxHealth(currentTeam, 20);
          healWithoutWarp(currentTeam, 20);
        }
      }
      case CORNFLOWER -> {
        queueHealingSource(
            currentTeam,
            scaleAbilityAmount(level, pos, (currentTeam.getHealth() / 4) * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
            true,
            level,
            pos);
      }
      case PINK_PETALS -> currentTeam.queueHealingAlsoIncreasesMaxHealth();
      case CARVED_PUMPKIN -> {
        if (level != null && pos != null) {
          queueHealingSource(
              currentTeam,
              scaleAbilityAmount(level, pos, countNearbyBlocks(level, pos, 1) * 4 * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
              true,
              level,
              pos);

          if (isBlock(level, pos.below(), BattleBlockIDs.IRON_BLOCK)) {
            queueShieldSource(
                currentTeam,
                scaleAbilityAmount(level, pos, 5 * effectMultiplier, BattleBlockIDs.BLUE_CARPET),
                true,
                level,
                pos);
            queueDamageSource(
                currentTeam,
                scaleAbilityAmount(level, pos, 4 * effectMultiplier, BattleBlockIDs.RED_CARPET),
                true,
                level,
                pos);
          }

          if (isBlock(level, pos.below(), BattleBlockIDs.SNOW)) {
            applySnowPumpkinCombo(currentTeam, enemyTeam, level, pos.below(), effectMultiplier);
          }
        }
      }
      case CHERRY_LEAVES -> {
        queueHealingSource(
            currentTeam,
            scaleAbilityAmount(level, pos, (currentTeam.getMaxHealth() / 10) * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
            true,
            level,
            pos);
      }
      case MOSS_BLOCK -> {
        if (level != null && pos != null) {
          replaceNearbyBlocksWithGrass(level, pos, 1);
        }
      }
      case NETHERRACK -> {
        queueDamageSource(
            currentTeam,
            scaleAbilityAmount(level, pos, currentTeam.getLastDamageDealt() * effectMultiplier, BattleBlockIDs.RED_CARPET),
            true,
            level,
            pos);
      }
      case VAULT -> {
        if (placingPlayer != null) {
          openVaultChoice(placingPlayer, currentTeam);
        } else if (currentTeam.removeRandomCardFromDeck(random) != null) {
          applyImmediateShieldGain(currentTeam, scaleAbilityAmount(level, pos, 8 * effectMultiplier, BattleBlockIDs.BLUE_CARPET));
          applyImmediateHealing(currentTeam, enemyTeam, scaleAbilityAmount(level, pos, 8 * effectMultiplier, BattleBlockIDs.GREEN_CARPET));
        }
      }
      case SCULK -> changeMaxHealth(enemyTeam, -2 * effectMultiplier);
      case BEACON -> {
        if (level != null && pos != null && isBlock(level, pos.below(), BattleBlockIDs.GOLD_BLOCK)) {
          queueShieldSource(
              currentTeam,
              scaleAbilityAmount(level, pos, 4 * effectMultiplier, BattleBlockIDs.BLUE_CARPET),
              true,
              level,
              pos);
        }
      }
      case END_CRYSTAL -> {
        if (!areExplosionsDisabled() && level != null && pos != null) {
          breakBlocksInCube(level, pos, 2, true);
          return new PlacedBlockResolution(null, battleBlock, List.of());
        }
      }
      case FURNACE -> {
        if (level != null && pos != null) {
          queueShieldSource(
              currentTeam,
              scaleAbilityAmount(level, pos, countNearbyBlocks(level, pos, 1) * effectMultiplier, BattleBlockIDs.BLUE_CARPET),
              true,
              level,
              pos);
        }
      }
      case LAPIS_BLOCK -> {
        queueHealingSource(
            currentTeam,
            scaleAbilityAmount(level, pos, ((currentTeam.getHealth() + 1) / 2) * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
            true,
            level,
            pos);
        replaceOneCardInDeckAndOnBoard(currentTeam, CreateBlocks.LAPIS_BLOCK, level, pos);
        return new PlacedBlockResolution(CreateBlocks.DIRT, battleBlock, List.of());
      }
      case DEEPSLATE_BRICKS -> {
        queueDamageSource(
            currentTeam,
            scaleAbilityAmount(level, pos, currentTeam.getShield() * 2 * effectMultiplier, BattleBlockIDs.RED_CARPET),
            true,
            level,
            pos);
      }
      case REINFORCED_DEEPSLATE -> {
        queueShieldSource(
            currentTeam,
            scaleAbilityAmount(level, pos, Math.min(currentTeam.getShield(), 10) * effectMultiplier, BattleBlockIDs.BLUE_CARPET),
            true,
            level,
            pos);
      }
      case DEEPSLATE_GOLD_ORE -> {
        int stolenShield = enemyTeam.getShield() / 2;
        enemyTeam.loseShield(stolenShield);
        queueShieldSource(
            currentTeam,
            scaleAbilityAmount(level, pos, stolenShield * effectMultiplier, BattleBlockIDs.BLUE_CARPET),
            true,
            level,
            pos);
      }
      case WITHER_SKELETON_SKULL -> {
        if (enemyTeam.getShield() > 6) {
          queueDamageSource(
              currentTeam,
              scaleAbilityAmount(level, pos, 8 * effectMultiplier, BattleBlockIDs.RED_CARPET),
              true,
              level,
              pos);
        }
      }
      case WITHER_ROSE -> {
        if (level != null && pos != null) {
          int replacedGrassBlocks = replaceNearbyGrassWithSoulSand(level, pos, 1);

          if (replacedGrassBlocks > 0) {
            queueDamageSource(
                currentTeam,
                scaleAbilityAmount(level, pos, replacedGrassBlocks * 3 * effectMultiplier, BattleBlockIDs.RED_CARPET),
                true,
                level,
                pos);
            queueHealingSource(
                currentTeam,
                scaleAbilityAmount(level, pos, replacedGrassBlocks * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
                true,
                level,
                pos);
          }
        }
      }
      case LECTERN -> {
        queueDamageSource(
            currentTeam,
            scaleAbilityAmount(level, pos, currentTeam.getHandSize() * 8 * effectMultiplier, BattleBlockIDs.RED_CARPET),
            true,
            level,
            pos);
      }
      case SMITHING_TABLE -> {
        queueDamageSource(
            currentTeam,
            scaleAbilityAmount(level, pos, countBlocksOnBoard(Classification.MAN_MADE, battleBlock) * effectMultiplier, BattleBlockIDs.RED_CARPET),
            true,
            level,
            pos);
      }
      case SOUL_TORCH -> currentTeam.queueTurnHealingBonus(4 * effectMultiplier);
      case REDSTONE_TORCH -> currentTeam.queueTurnDamageBonus(5 * effectMultiplier);
      case TORCH -> currentTeam.queueTurnShieldBonus(2 * effectMultiplier);
      case COPPER_TORCH -> currentTeam.queueIgnoreNextIncomingDamage();
      case CRIMSON_HYPHAE -> {
        queueDamageSource(
            currentTeam,
            scaleAbilityAmount(level, pos, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock) * effectMultiplier, BattleBlockIDs.RED_CARPET),
            true,
            level,
            pos);
      }
      case WARPED_HYPHAE -> {
        queueHealingSource(
            currentTeam,
            scaleAbilityAmount(level, pos, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock) * effectMultiplier, BattleBlockIDs.GREEN_CARPET),
            true,
            level,
            pos);
      }
      case RAW_IRON_BLOCK -> currentTeam.queueTurnDamage(30 * effectMultiplier);
      case RAW_GOLD_BLOCK -> currentTeam.queueTurnShieldGain(6 * effectMultiplier);
      case RAW_COPPER_BLOCK -> currentTeam.queueTurnHealing(26 * effectMultiplier);
      case NETHERITE_BLOCK -> {
        replaceOneCardInDeckAndOnBoard(currentTeam, CreateBlocks.NETHERITE_BLOCK, level, pos);
        return new PlacedBlockResolution(CreateBlocks.DIRT, battleBlock, List.of());
      }
      case CHEST -> {
        if (level != null && pos != null) {
          activateChestAbility(currentTeam, enemyTeam, level, pos);
        }
      }
      case TRAPPED_CHEST -> {
        if (level != null && pos != null) {
          return new PlacedBlockResolution(
              battleBlock,
              battleBlock,
              collectTrappedChestStoredBlocks(currentTeam, level, pos));
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
          queueDamageSource(
              currentTeam,
              scaleAbilityAmount(level, pos, 8 * effectMultiplier, BattleBlockIDs.RED_CARPET),
              true,
              level,
              pos);
        }
      }
      case SOUL_SAND -> enemyTeam.queueTurnDrawModifier(-1 * effectMultiplier);
      case BOOKSHELF -> currentTeam.queueDrawEntireDeckNextTurn();
      case CHISELED_BOOKSHELF -> {
        if (placingPlayer != null) {
          openChiseledBookshelfChoice(placingPlayer, currentTeam, 2 * effectMultiplier);
        } else {
          queueRandomBattleCardsForNextHand(currentTeam, 2 * effectMultiplier);
        }
      }
      case STONE_BRICKS -> currentTeam.setShield(0);
      case CRACKED_STONE_BRICKS -> enemyTeam.setShield(0);
      case COBBLESTONE -> {
        enemyTeam.setShield(0);
        enemyTeam.disableShieldForTurns(2);
      }
      case POLISHED_BLACKSTONE_BRICKS -> {
        if (level != null && pos != null) {
          queueDamageSource(
              currentTeam,
              scaleAbilityAmount(level, pos, countAdjacentOccupiedBlocks(level, pos) * 3 * effectMultiplier, BattleBlockIDs.RED_CARPET),
              true,
              level,
              pos);
        }
      }
      case GLASS -> {
        BattleBlock copiedBlock = battleState.getLastPlacedBlock();

        if (copiedBlock != null && copiedBlock != battleBlock) {
          return copyBattleBlockPlacement(copiedBlock, battleBlock, currentTeam, enemyTeam, level, pos);
        }
      }
      case GLASS_PANE -> {
        BattleBlock copiedBlock = battleState.getLastPlacedBlock(currentTeam.getSide().otherSide());

        if (copiedBlock != null && copiedBlock != battleBlock) {
          return copyBattleBlockPlacement(copiedBlock, battleBlock, currentTeam, enemyTeam, level, pos);
        }
      }
      case COAL_BLOCK -> {
      }
      case GOLD_BLOCK -> {
        if (level != null && pos != null && hasAdjacent(level, pos, BattleBlockIDs.PLAYER_HEAD)) {
          replaceNearbyBlocksWithGold(level, pos, 2);
        }

        int convertedDefence = currentTeam.getShield() * 3 * effectMultiplier;
        currentTeam.setShield(0);
        changeMaxHealth(currentTeam, convertedDefence);
        healWithoutWarp(currentTeam, convertedDefence);
        replaceOneCardInDeckAndOnBoard(currentTeam, CreateBlocks.GOLD_BLOCK, level, pos);
        return new PlacedBlockResolution(CreateBlocks.DIRT, battleBlock, List.of());
      }
      case EMERALD_BLOCK -> {
        currentTeam.queueTurnDrawModifier(2 * effectMultiplier);
        currentTeam.queueTurnExtraPlacements(effectMultiplier);
        replaceOneCardInDeckAndOnBoard(currentTeam, CreateBlocks.EMERALD_BLOCK, level, pos);
        return new PlacedBlockResolution(CreateBlocks.DIRT, battleBlock, List.of());
      }
      case REDSTONE_BLOCK -> {
        for (int i = 0; i < effectMultiplier; i++) {
          activateAdjacentBlocks(level, pos);
        }
      }
      case DIAMOND_BLOCK -> {
        replaceOneCardInDeckAndOnBoard(currentTeam, CreateBlocks.DIAMOND_BLOCK, level, pos);
        return new PlacedBlockResolution(CreateBlocks.DIRT, battleBlock, List.of());
      }
      case OBSIDIAN -> {
      }
      case CRYING_OBSIDIAN -> {
        if (level != null && pos != null) {
          return new PlacedBlockResolution(
              battleBlock,
              battleBlock,
              collectBrokenFriendlyOtherworldlyBlocks(currentTeam, level, pos));
        }
      }
      case BEDROCK -> currentTeam.queueSkipNextTurn();
      case RED_BED -> {
        if (doBedsExplodeOnPlacement()) {
          if (level != null && pos != null) {
            breakBlocksAround(level, pos);
          }

          return new PlacedBlockResolution(null, battleBlock, List.of());
        }

        if (isWarpActive(BattleWarp.NIGHT)) {
          battleState.setActiveWarp(BattleWarp.NONE);
          battleState.setActiveWarpStarterSide(null);
          battleState.resetActiveWarpTurnCount();
          battleState.resetActiveWarpRoundCount();
          battleState.suppressNightWarpOnce();
        }

        for (int i = 0; i < effectMultiplier; i++) {
          queuePerTurnEffects(currentTeam, enemyTeam);
        }
      }
      case PLAYER_HEAD -> {
        currentTeam.queueExtraTurn();

        if (level != null && pos != null && hasAdjacent(level, pos, BattleBlockIDs.GOLD_BLOCK)) {
          replaceNearbyBlocksWithGold(level, pos, 2);
        }
      }
      case CREEPER_HEAD -> {
        if (level != null && pos != null) {
          breakBattleBlockAt(level, pos.below());
        }
      }
      case OAK_PLANKS -> {
        for (int i = 0; i < 4 * effectMultiplier; i++) {
          currentTeam.queueNextHandCard(CreateBlocks.OAK_PLANKS);
        }
      }
      case PALE_MOSS_BLOCK -> {
        if (level != null && pos != null) {
          placePaleMossCarpets(currentTeam, level, pos);
        }
      }
      default -> {
      }
    }

    return new PlacedBlockResolution(battleBlock, battleBlock, List.of());
  }

  private boolean placeBattleBlock(
      BattleBlock battleBlock,
      TeamSide actingSide,
      ServerLevel level,
      BlockPos pos,
      ServerPlayer placingPlayer) {
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

    PlacedBlockResolution placementResult = applyOnPlaceAbility(
        battleBlock,
        currentTeam,
        enemyTeam,
        level,
        pos,
        placingPlayer);

    queueImmediateBlockEffects(placementResult.effectBlock(), currentTeam, level, pos);

    if (level != null && pos != null && placementResult.boardBlock() != null && isWarpActive(BattleWarp.REDSTONE)) {
      activatePlacedBlockNow(new OwnedPlacedBattleBlock(
          currentTeam,
          new PlacedBattleBlock(level, pos, placementResult.boardBlock())));
    }

    BattleCombatLog.logBlockPlaced(resolveServer(), resolvedActingSide, battleBlock.displayName);

    boolean shouldEndTurn = battleState.consumePlacementThisTurn() <= 0;

    if (shouldEndTurn) {
      endTurn(resolvedActingSide);
    }

    if (level != null && pos != null && placementResult.boardBlock() != null) {
      registerPlacedBlock(placementResult, level, pos, resolvedActingSide);
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

    int statBonus = getPodzolStatBonus(currentTeam, level, pos) - getPaleMossPenalty(level, pos);
    int damage = battleBlock.damage + statBonus;
    int healing = battleBlock.healing + statBonus;
    int shield = battleBlock.defence + statBonus;
    int shieldDamage = battleBlock.defenceDamage + statBonus;

    if (battleBlock.id == BattleBlockIDs.ENCHANTING_TABLE
        && hasNearbyWorldBlock(level, pos, 2, BattleBlockIDs.BOOKSHELF)) {
      healing += 3;
    }

    if (battleBlock.id == BattleBlockIDs.CRACKED_STONE_BRICKS) {
      shieldDamage = 0;
    }

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
        queueDirectHealthDamageSource(currentTeam, damage, false, level, pos);
      } else {
        queueDamageSource(currentTeam, damage, false, level, pos);
      }
    }

    if (!battleBlock.healingPerTurn && healing != 0) {
      queueHealingSource(currentTeam, healing, false, level, pos);
    }

    if (!battleBlock.defencePerTurn && shield != 0) {
      queueShieldSource(currentTeam, shield, false, level, pos);
    }

    if (!battleBlock.defenceDamagePerTurn && shieldDamage != 0) {
      queueShieldDamageSource(currentTeam, shieldDamage, level, pos);
    }
  }

  private void registerPlacedBlock(PlacedBlockResolution placementResult, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    PlacedBattleBlock placedBlock = new PlacedBattleBlock(level, pos, placementResult.boardBlock());
    placedBlock.addStoredBlocks(placementResult.storedBlocks());
    getTeamForTurn(actingSide).addPlacedBlock(placedBlock);
    battleState.recordLastPlacedBlock(actingSide, placementResult.boardBlock());
  }

  private boolean hasPerTurnEffect(BattleBlock battleBlock) {
    return battleBlock.damagePerTurn
        || battleBlock.healingPerTurn
        || battleBlock.defencePerTurn
        || battleBlock.defenceDamagePerTurn;
  }

  private void queueDamageSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    queueDamageSource(currentTeam, amount, applyBonusEvenWhenZero, null, null);
  }

  private void queueDamageSource(
      BattleTeam currentTeam,
      int amount,
      boolean applyBonusEvenWhenZero,
      ServerLevel sourceLevel,
      BlockPos sourcePos) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int resolvedAmount = adjustDamageAmount(amount + currentTeam.getActiveTurnDamageBonus());
      pendingDamage += resolvedAmount;
      emitBlockActivation(currentTeam, sourceLevel, sourcePos, BattleFeedbackType.DAMAGE, resolvedAmount);
    }
  }

  private void queueDirectHealthDamageSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    queueDirectHealthDamageSource(currentTeam, amount, applyBonusEvenWhenZero, null, null);
  }

  private void queueDirectHealthDamageSource(
      BattleTeam currentTeam,
      int amount,
      boolean applyBonusEvenWhenZero,
      ServerLevel sourceLevel,
      BlockPos sourcePos) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int resolvedAmount = adjustDamageAmount(amount + currentTeam.getActiveTurnDamageBonus());
      pendingDirectHealthDamage += resolvedAmount;
      emitBlockActivation(currentTeam, sourceLevel, sourcePos, BattleFeedbackType.DAMAGE, resolvedAmount);
    }
  }

  private void queueHealingSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    queueHealingSource(currentTeam, amount, applyBonusEvenWhenZero, null, null);
  }

  private void queueHealingSource(
      BattleTeam currentTeam,
      int amount,
      boolean applyBonusEvenWhenZero,
      ServerLevel sourceLevel,
      BlockPos sourcePos) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int totalHealing = adjustHealingAmount(amount + currentTeam.getActiveTurnHealingBonus());
      pendingHealing += totalHealing;
      emitBlockActivation(currentTeam, sourceLevel, sourcePos, BattleFeedbackType.HEALING, totalHealing);

      if (currentTeam.isActiveHealingAlsoIncreasesMaxHealth() && totalHealing > 0) {
        pendingMaxHealthGain += totalHealing;
      }

      if (isWarpActive(BattleWarp.LUSH_CAVE) && totalHealing > 0) {
        pendingDamage += totalHealing;
        emitBlockActivation(currentTeam, sourceLevel, sourcePos, BattleFeedbackType.DAMAGE, totalHealing);
      }
    }
  }

  private void queueShieldSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    queueShieldSource(currentTeam, amount, applyBonusEvenWhenZero, null, null);
  }

  private void queueShieldSource(
      BattleTeam currentTeam,
      int amount,
      boolean applyBonusEvenWhenZero,
      ServerLevel sourceLevel,
      BlockPos sourcePos) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int resolvedAmount = adjustShieldGainAmount(amount + currentTeam.getActiveTurnShieldBonus());
      pendingShieldGain += resolvedAmount;
      emitBlockActivation(currentTeam, sourceLevel, sourcePos, BattleFeedbackType.SHIELD_GAIN, resolvedAmount);
    }
  }

  private void queueShieldDamageSource(BattleTeam sourceTeam, int amount, ServerLevel sourceLevel, BlockPos sourcePos) {
    int resolvedAmount = adjustShieldDamageAmount(amount);

    if (resolvedAmount == 0) {
      return;
    }

    pendingShieldDamage += resolvedAmount;
    emitBlockActivation(sourceTeam, sourceLevel, sourcePos, BattleFeedbackType.SHIELD_LOSS, resolvedAmount);
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
          queueDirectHealthDamageSource(currentTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
        } else {
          queueDamageSource(currentTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
        }

        if (battleBlock.id == BattleBlockIDs.MYCELIUM) {
          queueHealingSource(
              currentTeam,
              Math.max(0, damageAmount - enemyTeam.getShield()),
              false,
              placedBlock.level(),
              placedBlock.pos());
        }
      }

      if (battleBlock.healingPerTurn) {
        queueHealingSource(currentTeam, getPerTurnHealingAmount(placedBlock, currentTeam), false, placedBlock.level(), placedBlock.pos());
      }

      if (battleBlock.defencePerTurn) {
        queueShieldSource(currentTeam, getPerTurnShieldAmount(placedBlock, currentTeam), false, placedBlock.level(), placedBlock.pos());
      }

      if (battleBlock.defenceDamagePerTurn) {
        queueShieldDamageSource(currentTeam, getPerTurnShieldDamageAmount(placedBlock, currentTeam), placedBlock.level(), placedBlock.pos());
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
          int damageAmount = isBlock(placedBlock.level(), placedBlock.pos().below(), BattleBlockIDs.CAULDRON)
              ? 8
              : 4;
          queueDamageSource(
              currentTeam,
              applyCarpetModifier(placedBlock, damageAmount, BattleBlockIDs.RED_CARPET),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }
      }
      case FURNACE -> {
        updateFurnaceDisplayState(placedBlock.level(), placedBlock.pos());

        if (hasAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.COAL_BLOCK)) {
          queueShieldSource(
              currentTeam,
              applyCarpetModifier(placedBlock, 5, BattleBlockIDs.BLUE_CARPET),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }

        if (hasAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.CAMPFIRE)) {
          queueHealingSource(
              currentTeam,
              applyCarpetModifier(placedBlock, 6, BattleBlockIDs.GREEN_CARPET),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }
      }
      case CAULDRON -> {
        if (isBlock(placedBlock.level(), placedBlock.pos().above(), BattleBlockIDs.WATER)) {
          queueHealingSource(
              currentTeam,
              applyCarpetModifier(placedBlock, 4, BattleBlockIDs.GREEN_CARPET),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }
      }
      case COMPOSTER, STONECUTTER, CRYING_OBSIDIAN -> activateStoredEffectsFromAbsorber(currentTeam, placedBlock);
      case CHERRY_LOG, JUNGLE_LOG, MUSHROOM_STEM -> {
        placedBlock.advanceOwnerTurnCount();

        if (Abilities.growBlockUpwardIfAir(placedBlock.level(), placedBlock.pos())) {
          PlacedBattleBlock grownBlock = new PlacedBattleBlock(
              placedBlock.level(),
              placedBlock.pos().above(),
              placedBlock.battleBlock());
          grownBlocks.add(grownBlock);
          onAnyBlockPlaced(placedBlock.level(), grownBlock.pos());
        }
      }
      case PALE_OAK_LOG -> {
        placedBlock.advanceOwnerTurnCount();

        if (tryGrowPaleOakLogAboveCreakingHeart(placedBlock.level(), placedBlock.pos(), grownBlocks)) {
          break;
        }

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
          queueHealingSource(currentTeam, 32, true, placedBlock.level(), placedBlock.pos());
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
      case CARVED_PUMPKIN -> {
        if (isBlock(placedBlock.level(), placedBlock.pos().below(), BattleBlockIDs.SNOW)) {
          queueDamageSource(
              currentTeam,
              applyCarpetModifier(placedBlock, 10, BattleBlockIDs.RED_CARPET),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }
      }
      case LAVA, COPPER_BLOCK, CHISELED_COPPER, COPPER_GRATE, COPPER_BULB -> placedBlock.advanceOwnerTurnCount();
      case CAKE -> {
        if (placedBlock.advanceOwnerTurnCount() >= 4) {
          currentTeam.getPlacedBlocks().remove(placedBlock);
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
          onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
        }
      }
      case CREEPER_HEAD -> {
        if (placedBlock.advanceOwnerTurnCount() >= 1) {
          currentTeam.getPlacedBlocks().remove(placedBlock);
          triggerExplosiveCombo(placedBlock.level(), placedBlock.pos(), 1, true, true);
          queueDamageSource(
              currentTeam,
              getExplosionDamageAmount(
                  placedBlock.level(),
                  placedBlock.pos(),
                  8 * getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock())),
              true,
              placedBlock.level(),
              placedBlock.pos());
        }
      }
      case SKELETON_SKULL, ZOMBIE_HEAD -> {
        if (isInSunlight(placedBlock.level(), placedBlock.pos())) {
          currentTeam.getPlacedBlocks().remove(placedBlock);
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
          onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
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
    if (isWarpActive(BattleWarp.WITCH_HUT) && battleState.getActiveWitchHutEffect() == WitchHutEffect.NO_SUNLIGHT) {
      return false;
    }

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

  private int getPaleMossPenalty(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return 0;
    }

    return isBlock(level, pos.above(), BattleBlockIDs.PALE_MOSS_CARPET) ? 2 : 0;
  }

  private int getImmediateDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int damageAmount = placedBlock.battleBlock().damage
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());
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

    int healingAmount = placedBlock.battleBlock().healing
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.ENCHANTING_TABLE
        && hasNearbyWorldBlock(placedBlock.level(), placedBlock.pos(), 2, BattleBlockIDs.BOOKSHELF)) {
      healingAmount += 3;
    }

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

    int shieldAmount = placedBlock.battleBlock().defence
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());

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

    int shieldDamageAmount = placedBlock.battleBlock().defenceDamage
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());
    return shieldDamageAmount * getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());
  }

  private void activatePlacedBlockNow(OwnedPlacedBattleBlock trackedBlock) {
    BattleTeam ownerTeam = trackedBlock.ownerTeam();
    BattleTeam enemyTeam = battleState.getOpponentOf(ownerTeam.getSide());
    PlacedBattleBlock placedBlock = trackedBlock.placedBlock();

    if (!canActivatePlacedBlock(ownerTeam, placedBlock)) {
      return;
    }

    applyOnPlaceAbility(placedBlock.battleBlock(), ownerTeam, enemyTeam, placedBlock.level(), placedBlock.pos(), null);

    if (placedBlock.battleBlock().damagePerTurn) {
      int damageAmount = getPerTurnDamageAmount(placedBlock, ownerTeam);

      if (placedBlock.battleBlock().id == BattleBlockIDs.SOUL_CAMPFIRE) {
        queueDirectHealthDamageSource(ownerTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
      } else {
        queueDamageSource(ownerTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
      }

      if (placedBlock.battleBlock().id == BattleBlockIDs.MYCELIUM) {
        queueHealingSource(
            ownerTeam,
            Math.max(0, damageAmount - enemyTeam.getShield()),
            false,
            placedBlock.level(),
            placedBlock.pos());
      }
    } else {
      int damageAmount = getImmediateDamageAmount(placedBlock, ownerTeam);

      if (damageAmount != 0) {
        if (placedBlock.battleBlock().id == BattleBlockIDs.SOUL_LANTERN) {
          queueDirectHealthDamageSource(ownerTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
        } else {
          queueDamageSource(ownerTeam, damageAmount, false, placedBlock.level(), placedBlock.pos());
        }
      }
    }

    if (placedBlock.battleBlock().healingPerTurn) {
      queueHealingSource(ownerTeam, getPerTurnHealingAmount(placedBlock, ownerTeam), false, placedBlock.level(), placedBlock.pos());
    } else {
      int healingAmount = getImmediateHealingAmount(placedBlock, ownerTeam);

      if (healingAmount != 0) {
        queueHealingSource(ownerTeam, healingAmount, false, placedBlock.level(), placedBlock.pos());
      }
    }

    if (placedBlock.battleBlock().defencePerTurn) {
      queueShieldSource(ownerTeam, getPerTurnShieldAmount(placedBlock, ownerTeam), false, placedBlock.level(), placedBlock.pos());
    } else {
      int shieldAmount = getImmediateShieldAmount(placedBlock, ownerTeam);

      if (shieldAmount != 0) {
        queueShieldSource(ownerTeam, shieldAmount, false, placedBlock.level(), placedBlock.pos());
      }
    }

    if (placedBlock.battleBlock().defenceDamagePerTurn) {
      queueShieldDamageSource(ownerTeam, getPerTurnShieldDamageAmount(placedBlock, ownerTeam), placedBlock.level(), placedBlock.pos());
    } else {
      int shieldDamageAmount = getImmediateShieldDamageAmount(placedBlock, ownerTeam);

      if (shieldDamageAmount != 0) {
        queueShieldDamageSource(ownerTeam, shieldDamageAmount, placedBlock.level(), placedBlock.pos());
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

  private void activateStoredEffectsFromAbsorber(BattleTeam ownerTeam, PlacedBattleBlock absorberBlock) {
    if (!absorberBlock.hasStoredBlocks()) {
      return;
    }

    for (BattleBlock storedBlock : absorberBlock.getStoredBlocks()) {
      activatePlacedBlockNow(new OwnedPlacedBattleBlock(
          ownerTeam,
          new PlacedBattleBlock(absorberBlock.level(), absorberBlock.pos(), storedBlock)));
    }
  }

  private ArrayList<BattleBlock> collectTrappedChestStoredBlocks(BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    ArrayList<OwnedPlacedBattleBlock> nearbyBlocks = getTrackedBlocksInCube(level, pos, 1, false);
    ArrayList<BattleBlock> storedBlocks = new ArrayList<>();

    for (OwnedPlacedBattleBlock trackedBlock : nearbyBlocks) {
      if (trackedBlock.placedBlock().battleBlock().classification != Classification.MAN_MADE) {
        continue;
      }

      if (trackedBlock.ownerTeam() == currentTeam) {
        storedBlocks.add(trackedBlock.placedBlock().battleBlock());
        removeTrackedBlockSilently(level, trackedBlock.placedBlock().pos());
        level.setBlock(trackedBlock.placedBlock().pos(), Blocks.AIR.defaultBlockState(), 3);
        onAnyBlockBroken(level, trackedBlock.placedBlock().pos());
        continue;
      }

      breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
    }

    return storedBlocks;
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

  private void activateAdjacentBlocks(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return;
    }

    for (Direction direction : Direction.values()) {
      OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos.relative(direction));

      if (trackedBlock != null) {
        activatePlacedBlockNow(trackedBlock);
      }
    }
  }

  private void openVaultChoice(ServerPlayer player, BattleTeam team) {
    List<BattleBlock> currentDeck = List.copyOf(team.getStartingDeck());

    if (currentDeck.isEmpty()) {
      player.sendSystemMessage(Component.literal("There are no cards left in that deck to remove."));
      return;
    }

    player.openMenu(new SimpleMenuProvider(
        (containerId, inventory, menuPlayer) -> new BattleAbilityChoiceMenu(
            containerId,
            inventory,
            currentDeck,
            1,
            Component.literal("Vault Choice").withStyle(net.minecraft.ChatFormatting.GOLD),
            List.of(
                Component.literal("Choose 1 card to permanently remove from your deck.").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.literal("If you choose one, gain 8 defence and 8 healing.").withStyle(net.minecraft.ChatFormatting.GRAY)),
            (serverPlayer, selectedBlocks) -> {
              if (!selectedBlocks.isEmpty() && resolveVaultChoice(team.getSide(), selectedBlocks.get(0))) {
                syncBattleHands(serverPlayer.level().getServer());
                BattleScoreboards.updateScoreboard(serverPlayer.level().getServer(), battleState);
                serverPlayer.sendSystemMessage(Component.literal("Vault removed " + selectedBlocks.get(0).displayName + " from the deck."));
              }
            }),
        Component.literal("Vault Choice")));
  }

  private void openChiseledBookshelfChoice(ServerPlayer player, BattleTeam team, int amount) {
    player.openMenu(new SimpleMenuProvider(
        (containerId, inventory, menuPlayer) -> new BattleAbilityChoiceMenu(
            containerId,
            inventory,
            CreateBlocks.ALL,
            amount,
            Component.literal("Chiseled Bookshelf").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
            List.of(
                Component.literal("Choose " + amount + " cards to add to your next hand.").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.literal("Any battle block can be chosen here.").withStyle(net.minecraft.ChatFormatting.GRAY)),
            (serverPlayer, selectedBlocks) -> {
              queueChosenBlocksForNextHand(team.getSide(), selectedBlocks);
              serverPlayer.sendSystemMessage(Component.literal(selectedBlocks.size() + " cards were queued for your next hand."));
            }),
        Component.literal("Chiseled Bookshelf")));
  }

  private void openDeepDarkChoice(ServerPlayer player, BattleTeam team) {
    List<BattleBlock> currentDeck = List.copyOf(team.getStartingDeck());

    if (currentDeck.isEmpty()) {
      player.sendSystemMessage(Component.literal("There are no cards left in that deck to remove."));
      return;
    }

    player.openMenu(new SimpleMenuProvider(
        (containerId, inventory, menuPlayer) -> new BattleAbilityChoiceMenu(
            containerId,
            inventory,
            currentDeck,
            1,
            Component.literal("Deep Dark Choice").withStyle(net.minecraft.ChatFormatting.DARK_AQUA),
            List.of(
                Component.literal("Choose 1 card to permanently remove from your deck.").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.literal("If you do not have a deck card to remove, you take 30 damage instead.").withStyle(net.minecraft.ChatFormatting.GRAY)),
            (serverPlayer, selectedBlocks) -> {
              if (!selectedBlocks.isEmpty() && resolveDeepDarkChoice(team.getSide(), selectedBlocks.get(0))) {
                syncBattleHands(serverPlayer.level().getServer());
                BattleScoreboards.updateScoreboard(serverPlayer.level().getServer(), battleState);
                serverPlayer.sendSystemMessage(Component.literal("Deep Dark removed " + selectedBlocks.get(0).displayName + " from the deck."));
              }
            },
            serverPlayer -> {
              dealDamageToTeam(team, 30);
              BattleScoreboards.updateScoreboard(serverPlayer.level().getServer(), battleState);
              serverPlayer.sendSystemMessage(Component.literal("Deep Dark choice cancelled. You took 30 damage."));
            }),
        Component.literal("Deep Dark Choice")));
  }

  public boolean resolveVaultChoice(TeamSide side, BattleBlock selectedBlock) {
    BattleTeam team = battleState.getTeam(side);

    if (!team.removeOneCardFromDeck(selectedBlock)) {
      return false;
    }

    applyImmediateShieldGain(team, 8);
    applyImmediateHealing(team, battleState.getOpponentOf(side), 8);
    return true;
  }

  public boolean resolveDeepDarkChoice(TeamSide side, BattleBlock selectedBlock) {
    return battleState.getTeam(side).removeOneCardFromDeck(selectedBlock);
  }

  public void queueChosenBlocksForNextHand(TeamSide side, List<BattleBlock> chosenBlocks) {
    BattleTeam team = battleState.getTeam(side);

    for (BattleBlock chosenBlock : chosenBlocks) {
      team.queueNextHandCard(chosenBlock);
    }
  }

  private ServerPlayer findOnlinePlayerForTeam(TeamSide side) {
    if (lastKnownServer == null) {
      return null;
    }

    for (ServerPlayer player : lastKnownServer.getPlayerList().getPlayers()) {
      if (BattlePlayerTeams.getTeamSide(player).orElse(null) == side) {
        return player;
      }
    }

    return null;
  }

  private void swapTeamDeckStates(BattleTeam firstTeam, BattleTeam secondTeam) {
    ArrayList<BattleBlock> firstStartingDeck = new ArrayList<>(firstTeam.getStartingDeck());
    ArrayList<BattleBlock> firstDrawPile = new ArrayList<>(firstTeam.getDrawPile());
    ArrayList<BattleBlock> firstHand = new ArrayList<>(firstTeam.getHand());
    ArrayList<BattleBlock> secondStartingDeck = new ArrayList<>(secondTeam.getStartingDeck());
    ArrayList<BattleBlock> secondDrawPile = new ArrayList<>(secondTeam.getDrawPile());
    ArrayList<BattleBlock> secondHand = new ArrayList<>(secondTeam.getHand());

    firstTeam.replaceDeckState(secondStartingDeck, secondDrawPile, secondHand);
    secondTeam.replaceDeckState(firstStartingDeck, firstDrawPile, firstHand);
  }

  private void applyWitchHutEffectOneTime(WitchHutEffect effect) {
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    switch (effect) {
      case REMOVE_DECK_CARD -> {
        removeRandomDeckCardForWitchHut(redTeam);
        removeRandomDeckCardForWitchHut(blueTeam);
      }
      case SWAP_DECKS -> {
        swapTeamDeckStates(redTeam, blueTeam);
        BattleCombatLog.broadcast(
            resolveServer(),
            Component.literal("Witch Hut swapped both teams' decks.")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
      }
      default -> {
      }
    }
  }

  private void removeRandomDeckCardForWitchHut(BattleTeam team) {
    BattleBlock removedBlock = team.removeRandomCardFromDeck(random);

    if (removedBlock == null) {
      return;
    }

    ServerPlayer player = findOnlinePlayerForTeam(team.getSide());

    if (player != null) {
      player.sendSystemMessage(Component.literal(
          "Witch Hut removed " + removedBlock.displayName + " from your deck.")
          .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }
  }

  private void rollWitchHutEffectIfNeeded(TeamSide activeSide) {
    if (!isWarpActive(BattleWarp.WITCH_HUT)) {
      battleState.setActiveWitchHutEffect(WitchHutEffect.NONE);
      return;
    }

    if (battleState.getActiveWitchHutEffect() != WitchHutEffect.NONE
        && (battleState.getActiveWarpStarterSide() == null
            || battleState.getActiveWarpStarterSide() != activeSide
            || battleState.getActiveWarpTurnCount() <= 0)) {
      return;
    }

    WitchHutEffect newEffect = WitchHutEffect.randomRoll(random);
    battleState.setActiveWitchHutEffect(newEffect);
    applyWitchHutEffectOneTime(newEffect);
    BattleCombatLog.logWitchHutEffect(resolveServer(), newEffect);

    if (lastKnownServer != null) {
      syncBattleHands(lastKnownServer);
      BattleScoreboards.updateScoreboard(lastKnownServer, battleState);
    }
  }

  private boolean isDamagePreventedByWitchHut() {
    return isWarpActive(BattleWarp.WITCH_HUT)
        && battleState.getActiveWitchHutEffect() == WitchHutEffect.NO_DAMAGE;
  }

  public boolean openStoredBlocksMenu(ServerPlayer player, ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock == null || !trackedBlock.placedBlock().hasStoredBlocks()) {
      return false;
    }

    BattleBlockIDs blockId = trackedBlock.placedBlock().battleBlock().id;

    if (blockId != BattleBlockIDs.TRAPPED_CHEST
        && blockId != BattleBlockIDs.CRYING_OBSIDIAN
        && blockId != BattleBlockIDs.COMPOSTER
        && blockId != BattleBlockIDs.STONECUTTER) {
      return false;
    }

    Component title = Component.literal(
        trackedBlock.placedBlock().battleBlock().displayName + " Contents")
        .withStyle(switch (blockId) {
          case TRAPPED_CHEST -> net.minecraft.ChatFormatting.GOLD;
          case CRYING_OBSIDIAN -> net.minecraft.ChatFormatting.LIGHT_PURPLE;
          case COMPOSTER -> net.minecraft.ChatFormatting.GREEN;
          case STONECUTTER -> net.minecraft.ChatFormatting.GRAY;
          default -> net.minecraft.ChatFormatting.WHITE;
        });

    player.openMenu(new SimpleMenuProvider(
        (containerId, inventory, menuPlayer) -> new BattleStoredBlocksMenu(
            containerId,
            inventory,
            title,
            trackedBlock.placedBlock().getStoredBlocks()),
        title));
    return true;
  }

  private void queueRandomBattleCardsForNextHand(BattleTeam team, int amount) {
    ArrayList<BattleBlock> selectableBlocks = new ArrayList<>(CreateBlocks.ALL);

    if (selectableBlocks.isEmpty()) {
      return;
    }

    for (int i = 0; i < amount; i++) {
      team.queueNextHandCard(selectableBlocks.get(random.nextInt(selectableBlocks.size())));
    }
  }

  private void replaceOneCardInDeckAndOnBoard(BattleTeam team, BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    team.replaceOneCardInDeck(battleBlock, CreateBlocks.DIRT);

    if (level != null && pos != null) {
      level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
    }
  }

  private int scaleAbilityAmount(ServerLevel level, BlockPos pos, int amount, BattleBlockIDs carpetId) {
    if (level == null || pos == null || amount == 0) {
      return amount;
    }

    return getBlockId(level, pos.above()).equals(carpetId.getId()) ? amount * 2 : amount;
  }

  private int countAdjacentOccupiedBlocks(ServerLevel level, BlockPos pos) {
    int count = 0;

    for (Direction direction : Direction.values()) {
      if (!level.getBlockState(pos.relative(direction)).isAir()) {
        count++;
      }
    }

    return count;
  }

  private PlacedBlockResolution copyBattleBlockPlacement(
      BattleBlock copiedBlock,
      BattleBlock originalBlock,
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      ServerLevel level,
      BlockPos pos) {
    if (level == null || pos == null || copiedBlock == null) {
      return new PlacedBlockResolution(originalBlock, originalBlock, List.of());
    }

    setLevelBlockToBattleBlock(level, pos, copiedBlock);

    if (copiedBlock.id == BattleBlockIDs.GLASS || copiedBlock.id == BattleBlockIDs.GLASS_PANE) {
      return new PlacedBlockResolution(copiedBlock, copiedBlock, List.of());
    }

    return applyOnPlaceAbility(copiedBlock, currentTeam, enemyTeam, level, pos, null);
  }

  private void setLevelBlockToBattleBlock(ServerLevel level, BlockPos pos, BattleBlock battleBlock) {
    if (level == null || pos == null || battleBlock == null) {
      return;
    }

    level.setBlock(
        pos,
        BuiltInRegistries.BLOCK.get(Identifier.parse(battleBlock.id.getId()))
            .map(reference -> reference.value().defaultBlockState())
            .orElse(Blocks.AIR.defaultBlockState()),
        3);
  }

  private ArrayList<BattleBlock> collectBrokenFriendlyOtherworldlyBlocks(BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    ArrayList<OwnedPlacedBattleBlock> nearbyBlocks = getTrackedBlocksInCube(level, pos, 1, false);
    ArrayList<OwnedPlacedBattleBlock> copiedFriendlyBlocks = new ArrayList<>();
    ArrayList<BattleBlock> storedBlocks = new ArrayList<>();

    for (OwnedPlacedBattleBlock trackedBlock : nearbyBlocks) {
      if (trackedBlock.placedBlock().battleBlock().classification != Classification.OTHERWORLDLY) {
        continue;
      }

      if (trackedBlock.ownerTeam() == currentTeam) {
        copiedFriendlyBlocks.add(trackedBlock);
        storedBlocks.add(trackedBlock.placedBlock().battleBlock());
      }

      breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
    }

    for (OwnedPlacedBattleBlock copiedFriendlyBlock : copiedFriendlyBlocks) {
      activatePlacedBlockNow(new OwnedPlacedBattleBlock(currentTeam, copiedFriendlyBlock.placedBlock()));
    }

    return storedBlocks;
  }

  private void placePaleMossCarpets(BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos supportPos = pos.relative(direction);
      BlockPos carpetPos = supportPos.above();

      if (level.getBlockState(supportPos).isAir() || !level.getBlockState(carpetPos).isAir()) {
        continue;
      }

      level.setBlock(carpetPos, Blocks.PALE_MOSS_CARPET.defaultBlockState(), 3);
      currentTeam.addPlacedBlock(new PlacedBattleBlock(level, carpetPos, CreateBlocks.PALE_MOSS_CARPET));
      onAnyBlockPlaced(level, carpetPos);
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
      changeMaxHealth(currentTeam, pendingMaxHealthGain);
    }

    applyImmediateHealing(currentTeam, enemyTeam, pendingHealing);
    applyImmediateShieldGain(currentTeam, pendingShieldGain);

    int actualDamageDealt = 0;

    if (enemyTeam.consumeIgnoreNextIncomingDamage()) {
      pendingDamage = 0;
      pendingDirectHealthDamage = 0;
      pendingShieldDamage = 0;
    }

    if (isDamagePreventedByWitchHut()) {
      pendingDamage = 0;
      pendingDirectHealthDamage = 0;
      pendingShieldDamage = 0;
    }

    if (shouldIgnoreDefenceForDamage()) {
      pendingDirectHealthDamage += pendingDamage;
      pendingDamage = 0;
    }

    int shieldBefore = enemyTeam.getShield();
    int healthBefore = enemyTeam.getHealth();
    actualDamageDealt += enemyTeam.takeHealthDamage(applyBeaconDamageReduction(enemyTeam, pendingDamage));
    actualDamageDealt += enemyTeam.takeDirectHealthDamage(applyBeaconDamageReduction(enemyTeam, pendingDirectHealthDamage));
    enemyTeam.loseShield(applyBeaconDamageReduction(enemyTeam, pendingShieldDamage));
    emitStatFeedback(enemyTeam, BattleFeedbackType.SHIELD_LOSS, shieldBefore - enemyTeam.getShield());
    emitStatFeedback(enemyTeam, BattleFeedbackType.DAMAGE, healthBefore - enemyTeam.getHealth());
    refreshCombatPresentation();
    tryReviveIfPossible(enemyTeam);
    currentTeam.recordLastDamageDealt(actualDamageDealt);
    enemyTeam.recordLastDamageTakenFromOpponent(actualDamageDealt);
    applyNetherGoldOreRetaliation(enemyTeam, actualDamageDealt);
    applyCactusReflection(enemyTeam, currentTeam, actualDamageDealt);
    applyEndOfTurnWarpCosts(currentTeam, enemyTeam);
    currentTeam.advanceTurnStatuses();

    currentTeam.clearActiveTurnEffects();
    clearPendingEffects();
    currentTeam.clearHand();
    battleState.setActiveSide(currentTeam.consumeExtraTurn()
        ? resolvedActingSide
        : resolvedActingSide.otherSide());
    BattleCombatLog.logTurnEnd(resolveServer(), resolvedActingSide, battleState.getActiveSide());
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
    rememberServer(server);
    BattleCardItems.syncHands(server, battleState);
  }

  public void syncTrackedBattleBlocks(MinecraftServer server) {
    rememberServer(server);
    BattleBlockOutlinePayload payload = createBattleBlockOutlinePayload();

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      syncTrackedBattleBlocks(player, payload);
    }
  }

  public void syncTrackedBattleBlocks(ServerPlayer player) {
    rememberServer(player.level().getServer());
    syncTrackedBattleBlocks(player, createBattleBlockOutlinePayload());
  }

  public void syncBattlePlayerVitals(MinecraftServer server) {
    rememberServer(server);
    BattleFeedback.syncBattlePlayerVitals(server, battleState);
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
    refreshCombatPresentation();
  }

  public void setTeamMaxHealth(TeamSide side, int maxHealth) {
    battleState.getTeam(side).setMaxHealth(maxHealth);
    refreshCombatPresentation();
  }

  public void setTeamShield(TeamSide side, int shield) {
    battleState.getTeam(side).setShield(shield);
    refreshCombatPresentation();
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

    if (activeTeam.consumeSkipNextTurn()) {
      activeTeam.clearHand();
      activeTeam.clearActiveTurnEffects();
      battleState.setRemainingPlacementsThisTurn(0);
      battleState.setActiveSide(activeTeam.getSide().otherSide());
      beginActiveTurn();
      return;
    }

    activeTeam.activateQueuedTurnEffects();
    rollWitchHutEffectIfNeeded(activeTeam.getSide());

    if (advanceWarpRoundAndMaybeFinishGame(activeTeam.getSide())) {
      return;
    }

    int placementsThisTurn = getPlacementsPerTurnForNewTurn();
    battleState.setRemainingPlacementsThisTurn(placementsThisTurn);
    drawHandForTeam(activeTeam, getBaseHandSizeForNewTurn());

    if (battleState.getActiveWarp() != BattleWarp.NONE) {
      battleState.advanceActiveWarpTurnCount();
    }

    BattleCombatLog.logTurnStart(
        resolveServer(),
        activeTeam.getSide(),
        activeTeam.getHandSize(),
        battleState.getRemainingPlacementsThisTurn(),
        battleState.getActiveWarp(),
        battleState.getActiveWitchHutEffect());
  }

  private void drawHandForTeam(BattleTeam team) {
    drawHandForTeam(team, getBaseHandSizeForCurrentTurn(team));
  }

  private void drawHandForTeam(BattleTeam team, int baseHandSize) {
    team.clearHand();

    if (isWarpActive(BattleWarp.LIBRARY) || team.consumeActiveDrawEntireDeck()) {
      if (team.getDrawPile().isEmpty()) {
        team.refillDrawPile();
        team.shuffleDrawPile();
      }

      while (!team.getDrawPile().isEmpty()) {
        team.addCardToHand(team.getDrawPile().remove(0));
      }

      for (BattleBlock bonusCard : team.consumeQueuedNextHandCards()) {
        team.addCardToHand(bonusCard);
      }

      return;
    }

    deckManager.dealCards(team, Math.max(0,
        baseHandSize
            + team.getActiveTurnDrawModifier()
            + getWarpDrawModifier()
            + getBeaconEmeraldDrawBonus(team)
            + countBlocksOnBoard(team, BattleBlockIDs.SHULKER_BOX)
            - countBlocksOnBoard(battleState.getOpponentOf(team.getSide()), BattleBlockIDs.SNOW)));

    for (BattleBlock bonusCard : team.consumeQueuedNextHandCards()) {
      team.addCardToHand(bonusCard);
    }
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
    BattleCombatLog.logEndWarpFinish(resolveServer(), losingSide);
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
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.CRIMSON_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      damageAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.LAVA) {
      damageAmount = Math.min(12, 2 + (placedBlock.ownerTurnCount() * 2));

      if (isBlock(placedBlock.level(), placedBlock.pos().below(), BattleBlockIDs.CAULDRON)) {
        damageAmount *= 2;
      }
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.COPPER_GRATE) {
      damageAmount += getCopperOxidationStage(placedBlock);
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.HORN_CORAL_BLOCK
        && hasAdjacentWater(placedBlock.level(), placedBlock.pos())) {
      damageAmount += 7;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.SLIME_BLOCK
        && hasAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.SLIME_BLOCK)) {
      damageAmount += 2;
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
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.WARPED_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      healingAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.COPPER_BULB) {
      healingAmount += getCopperOxidationStage(placedBlock);
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.BUBBLE_CORAL_BLOCK
        && hasAdjacentWater(placedBlock.level(), placedBlock.pos())) {
      healingAmount += 8;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.MAGMA_BLOCK
        && isBlock(placedBlock.level(), placedBlock.pos().above(), BattleBlockIDs.WATER)) {
      healingAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.SPAWNER && isWarpActive(BattleWarp.NETHER_FORTRESS)) {
      healingAmount = (healingAmount * 3) / 2;
    }

    healingAmount *= getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());

    return applyCarpetModifier(placedBlock, healingAmount, BattleBlockIDs.GREEN_CARPET);
  }

  private int getPerTurnShieldAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    if (isCoralSuppressedByWarp(placedBlock.battleBlock().id)) {
      return 0;
    }

    int shieldAmount = placedBlock.battleBlock().defence
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.COPPER_BLOCK) {
      shieldAmount += getCopperOxidationStage(placedBlock);
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.END_STONE && isWarpActive(BattleWarp.BED_WARS)) {
      shieldAmount += 3;
    }

    if (placedBlock.battleBlock().id == BattleBlockIDs.TUBE_CORAL_BLOCK
        && hasAdjacentWater(placedBlock.level(), placedBlock.pos())) {
      shieldAmount += 2;
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

    int shieldDamageAmount = placedBlock.battleBlock().defenceDamage
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos())
        - getPaleMossPenalty(placedBlock.level(), placedBlock.pos());
    return shieldDamageAmount * getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());
  }

  private int getCopperOxidationStage(PlacedBattleBlock placedBlock) {
    return Math.min(3, Math.max(0, placedBlock.ownerTurnCount() + 1));
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

    if (isProtectedByNetheriteBeacon(level, pos)) {
      return false;
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
        pos), true);
  }

  private void triggerExplosiveCombo(
      ServerLevel level,
      BlockPos pos,
      int baseRadius,
      boolean includeCenter,
      boolean crossShapeWhenUnboosted) {
    if (hasExplosionBoardWipe(level, pos)) {
      breakAllTrackedBlocks(level);
      return;
    }

    if (hasLightningRodExplosionBoost(level, pos)) {
      breakBlocksInCube(level, pos, Math.max(baseRadius + 1, baseRadius * 2), includeCenter);
      return;
    }

    if (crossShapeWhenUnboosted) {
      breakBlocksAround(level, pos);
      return;
    }

    breakBlocksInCube(level, pos, baseRadius, includeCenter);
  }

  private int getExplosionDamageAmount(ServerLevel level, BlockPos pos, int baseDamage) {
    if (baseDamage <= 0) {
      return 0;
    }

    return hasLightningRodExplosionBoost(level, pos) ? baseDamage * 2 : baseDamage;
  }

  private boolean hasLightningRodExplosionBoost(ServerLevel level, BlockPos pos) {
    return level != null
        && pos != null
        && isBlock(level, pos.above(), BattleBlockIDs.LIGHTNING_ROD);
  }

  private boolean hasExplosionBoardWipe(ServerLevel level, BlockPos pos) {
    return hasLightningRodExplosionBoost(level, pos)
        && hasAdjacent(level, pos, BattleBlockIDs.REDSTONE_BLOCK);
  }

  private void breakAllTrackedBlocks(ServerLevel level) {
    ArrayList<BlockPos> trackedPositions = new ArrayList<>();

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
        if (placedBlock.level() == level && placedBlock.stillExists()) {
          trackedPositions.add(placedBlock.pos().immutable());
        }
      }
    }

    breakBlocksInPositions(level, trackedPositions, true);
  }

  private void breakBattleBlockAt(ServerLevel level, BlockPos pos) {
    breakBattleBlockAt(level, pos, false);
  }

  private void breakBattleBlockAt(ServerLevel level, BlockPos pos, boolean fromExplosion) {
    if ((fromExplosion && isExplosionProtected(level, pos)) || isProtectedByNetheriteBeacon(level, pos)) {
      return;
    }

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
      case COAL_BLOCK -> {
        breakBlocksInCube(placedBlock.level(), placedBlock.pos(), 2, false);
        dealImmediateDamage(ownerTeam, enemyTeam, 10);
      }
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
        int effectMultiplier = getBlockEffectMultiplier(placedBlock.level(), placedBlock.pos(), placedBlock.battleBlock());
        applyImmediateShieldGain(team, 4 * effectMultiplier);
        applyImmediateHealing(team, enemyTeam, 10 * effectMultiplier);
        dealImmediateDamage(team, enemyTeam, 10 * effectMultiplier);
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
    return battleBlockId == BattleBlockIDs.ANCIENT_DEBRIS
        || battleBlockId == BattleBlockIDs.NETHERITE_BLOCK
        || battleBlockId == BattleBlockIDs.BEDROCK;
  }

  private void dealImmediateDamage(BattleTeam attackingTeam, BattleTeam defendingTeam, int amount) {
    if (isDamagePreventedByWitchHut()) {
      return;
    }

    int shieldBefore = defendingTeam.getShield();
    int healthBefore = defendingTeam.getHealth();
    int adjustedAmount = applyBeaconDamageReduction(defendingTeam, adjustDamageAmount(amount));
    int actualDamageDealt = shouldIgnoreDefenceForDamage()
        ? defendingTeam.takeDirectHealthDamage(adjustedAmount)
        : defendingTeam.takeHealthDamage(adjustedAmount);
    emitStatFeedback(defendingTeam, BattleFeedbackType.SHIELD_LOSS, shieldBefore - defendingTeam.getShield());
    emitStatFeedback(defendingTeam, BattleFeedbackType.DAMAGE, healthBefore - defendingTeam.getHealth());
    refreshCombatPresentation();

    tryReviveIfPossible(defendingTeam);
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

  private int countSupportedBeacons(BattleTeam team, BattleBlockIDs supportBlockId) {
    int count = 0;
    Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();
    BattleTeam enemyTeam = battleState.getOpponentOf(team.getSide());

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!refreshPlacedBlock(team, enemyTeam, placedBlock)) {
        iterator.remove();
        continue;
      }

      if (placedBlock.battleBlock().id == BattleBlockIDs.BEACON
          && isBlock(placedBlock.level(), placedBlock.pos().below(), supportBlockId)) {
        count++;
      }
    }

    return count;
  }

  private int getBeaconEmeraldDrawBonus(BattleTeam team) {
    return countSupportedBeacons(team, BattleBlockIDs.EMERALD_BLOCK) * 2;
  }

  private int applyBeaconDamageReduction(BattleTeam team, int amount) {
    if (amount <= 0) {
      return amount;
    }

    return Math.max(0, amount - countSupportedBeacons(team, BattleBlockIDs.IRON_BLOCK));
  }

  public void onAnyBlockPlaced(ServerLevel level, BlockPos pos) {
    rememberServer(level.getServer());

    if (!battleState.isGameRunning()) {
      return;
    }

    refreshNearbyFurnaces(level, pos);
    triggerChiseledCopper(level, pos);
    triggerObsidianBoardWipe(level, pos);
    triggerCalibratedSculkSensors(level, pos);
    triggerSculkShriekers(level, pos);
    resolveBoardStateCombos(level, pos);
    reevaluateActiveWarp(level, pos);
    applyActiveWarpBoardEffectAt(level, pos);
    triggerRespawnAnchorCombos(level);
    triggerStoredEffectAbsorbers(level);
  }

  public void onAnyBlockBroken(ServerLevel level, BlockPos pos) {
    rememberServer(level.getServer());

    if (!battleState.isGameRunning()) {
      return;
    }

    refreshNearbyFurnaces(level, pos);
    triggerSculkSensors(level, pos);
    triggerSculkCatalysts(level, pos);
    triggerSculkShriekers(level, pos);
    resolveBoardStateCombos(level, pos);
    reevaluateActiveWarp(level, pos);
    triggerRespawnAnchorCombos(level);
    triggerStoredEffectAbsorbers(level);
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

  private void triggerChiseledCopper(ServerLevel level, BlockPos pos) {
    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 1, false)) {
      if (trackedBlock.placedBlock().battleBlock().id != BattleBlockIDs.CHISELED_COPPER) {
        continue;
      }

      int damageAmount = Math.min(3, Math.max(0, trackedBlock.placedBlock().ownerTurnCount())) * 3;

      if (damageAmount > 0) {
        dealImmediateDamage(trackedBlock.ownerTeam(), battleState.getOpponentOf(trackedBlock.ownerTeam().getSide()), damageAmount);
      }

      if (!hasAdjacentBlockId(level, trackedBlock.placedBlock().pos(), "minecraft:honeycomb_block")) {
        breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
      }
    }
  }

  private void triggerRespawnAnchorCombos(ServerLevel level) {
    if (level == null || isNetherWarpActive()) {
      return;
    }

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
        if (placedBlock.level() != level
            || !placedBlock.stillExists()
            || placedBlock.battleBlock().id != BattleBlockIDs.RESPAWN_ANCHOR
            || countAdjacent(level, placedBlock.pos(), BattleBlockIDs.GLOWSTONE) < 2) {
          continue;
        }

        breakBlocksInCube(level, placedBlock.pos(), 2, true);
        dealDamageToTeam(battleState.getRedTeam(), 5);
        dealDamageToTeam(battleState.getBlueTeam(), 5);
        return;
      }
    }
  }

  private void triggerObsidianBoardWipe(ServerLevel level, BlockPos changedPos) {
    if (level == null || changedPos == null) {
      return;
    }

    ArrayList<BlockPos> candidatePositions = new ArrayList<>();
    candidatePositions.add(changedPos.immutable());

    for (Direction direction : Direction.values()) {
      candidatePositions.add(changedPos.relative(direction).immutable());
    }

    for (BlockPos candidatePos : candidatePositions) {
      if (isBlock(level, candidatePos, BattleBlockIDs.OBSIDIAN)
          && hasLightningRodExplosionBoost(level, candidatePos)
          && hasAdjacent(level, candidatePos, BattleBlockIDs.REDSTONE_BLOCK)) {
        breakAllTrackedBlocks(level);
        return;
      }
    }
  }

  private void updateFurnaceDisplayState(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return;
    }

    if (hasAdjacent(level, pos, BattleBlockIDs.COAL_BLOCK)) {
      level.setBlock(pos, Blocks.BLAST_FURNACE.defaultBlockState(), 3);
      return;
    }

    if (hasAdjacent(level, pos, BattleBlockIDs.CAMPFIRE)) {
      level.setBlock(pos, Blocks.SMOKER.defaultBlockState(), 3);
      return;
    }

    level.setBlock(pos, Blocks.FURNACE.defaultBlockState(), 3);
  }

  private void refreshNearbyFurnaces(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return;
    }

    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, pos, 1, true)) {
      if (trackedBlock.placedBlock().battleBlock().id == BattleBlockIDs.FURNACE) {
        updateFurnaceDisplayState(level, trackedBlock.placedBlock().pos());
      }
    }
  }

  private void triggerStoredEffectAbsorbers(ServerLevel level) {
    if (level == null) {
      return;
    }

    while (triggerNextStoredEffectAbsorber(level)) {
    }
  }

  private boolean triggerNextStoredEffectAbsorber(ServerLevel level) {
    for (BattleTeam ownerTeam : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock absorberBlock : new ArrayList<>(ownerTeam.getPlacedBlocks())) {
        Classification absorbedClassification = getAbsorbedClassification(absorberBlock.battleBlock().id);

        if (absorbedClassification == null || absorberBlock.level() != level || !absorberBlock.stillExists()) {
          continue;
        }

        for (OwnedPlacedBattleBlock nearbyBlock : getTrackedBlocksInCube(level, absorberBlock.pos(), 1, false)) {
          if (!areAdjacent(absorberBlock.pos(), nearbyBlock.placedBlock().pos())
              || nearbyBlock.placedBlock().battleBlock().classification != absorbedClassification) {
            continue;
          }

          absorberBlock.addStoredBlock(nearbyBlock.placedBlock().battleBlock());
          activatePlacedBlockNow(new OwnedPlacedBattleBlock(ownerTeam, nearbyBlock.placedBlock()));
          breakBattleBlockAt(level, nearbyBlock.placedBlock().pos());
          return true;
        }
      }
    }

    return false;
  }

  private Classification getAbsorbedClassification(BattleBlockIDs absorberBlockId) {
    return switch (absorberBlockId) {
      case COMPOSTER -> Classification.NATURAL;
      case STONECUTTER -> Classification.CAVE;
      default -> null;
    };
  }

  private void resolveBoardStateCombos(ServerLevel level, BlockPos changedPos) {
    for (int pass = 0; pass < 3; pass++) {
      boolean changed = false;

      for (BlockPos candidatePos : BlockPos.betweenClosed(
          changedPos.offset(-2, -2, -2),
          changedPos.offset(2, 2, 2))) {
        changed |= applyBoardStateComboAt(level, candidatePos.immutable());
      }

      if (!changed) {
        return;
      }
    }
  }

  private boolean applyBoardStateComboAt(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.GRASS_BLOCK)) {
      if (hasAdjacentWater(level, pos)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.FARMLAND.defaultBlockState(), CreateBlocks.FARMLAND);
      }

      if (isBlock(level, pos.above(), BattleBlockIDs.MUSHROOM_STEM) || hasAdjacent(level, pos, BattleBlockIDs.MYCELIUM)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.MYCELIUM.defaultBlockState(), CreateBlocks.MYCELIUM);
      }

      if (hasNearbyWorldBlock(level, pos, 1, BattleBlockIDs.COMPOSTER)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.PODZOL.defaultBlockState(), CreateBlocks.PODZOL);
      }
    }

    if (isBlock(level, pos, BattleBlockIDs.DIRT)) {
      if (hasAdjacent(level, pos, BattleBlockIDs.MYCELIUM)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.MYCELIUM.defaultBlockState(), CreateBlocks.MYCELIUM);
      }

      if (hasAdjacent(level, pos, BattleBlockIDs.PODZOL)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.PODZOL.defaultBlockState(), CreateBlocks.PODZOL);
      }

      if (hasAdjacent(level, pos, BattleBlockIDs.GRASS_BLOCK)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.GRASS_BLOCK.defaultBlockState(), CreateBlocks.GRASS_BLOCK);
      }
    }

    if (isBlock(level, pos, BattleBlockIDs.NETHERRACK)) {
      if (isBlock(level, pos.above(), BattleBlockIDs.CRIMSON_HYPHAE)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.CRIMSON_NYLIUM.defaultBlockState(), CreateBlocks.CRIMSON_NYLIUM);
      }

      if (isBlock(level, pos.above(), BattleBlockIDs.WARPED_HYPHAE)) {
        return replaceWorldBlockWithBattleBlock(level, pos, Blocks.WARPED_NYLIUM.defaultBlockState(), CreateBlocks.WARPED_NYLIUM);
      }
    }

    if (isBlock(level, pos, BattleBlockIDs.LAVA) && hasAdjacentWater(level, pos)) {
      return replaceWorldBlockWithBattleBlock(level, pos, Blocks.OBSIDIAN.defaultBlockState(), CreateBlocks.OBSIDIAN);
    }

    return false;
  }

  private boolean replaceWorldBlockWithBattleBlock(
      ServerLevel level,
      BlockPos pos,
      net.minecraft.world.level.block.state.BlockState replacementState,
      BattleBlock replacementBattleBlock) {
    if (getBlockId(level, pos).equals(replacementBattleBlock.id.getId())) {
      return false;
    }

    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock != null) {
      trackedBlock.ownerTeam().getPlacedBlocks().remove(trackedBlock.placedBlock());
      trackedBlock.ownerTeam().addPlacedBlock(new PlacedBattleBlock(level, pos, replacementBattleBlock));
    }

    level.setBlock(pos, replacementState, 3);
    return true;
  }

  private void applySnowPumpkinCombo(
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      ServerLevel level,
      BlockPos snowPos,
      int effectMultiplier) {
    queueDamageSource(
        currentTeam,
        scaleAbilityAmount(level, snowPos, 4 * effectMultiplier, BattleBlockIDs.RED_CARPET),
        true);

    for (OwnedPlacedBattleBlock trackedBlock : getTrackedBlocksInCube(level, snowPos, 3, true)) {
      if (isMobHead(trackedBlock.placedBlock().battleBlock().id)) {
        breakBattleBlockAt(level, trackedBlock.placedBlock().pos());
      }
    }
  }

  private boolean tryGrowPaleOakLogAboveCreakingHeart(
      ServerLevel level,
      BlockPos paleOakLogPos,
      ArrayList<PlacedBattleBlock> grownBlocks) {
    BlockPos heartPos = paleOakLogPos.above();

    if (!isBlock(level, heartPos, BattleBlockIDs.CREAKING_HEART) || !level.getBlockState(heartPos.above()).isAir()) {
      return false;
    }

    if (!level.setBlock(heartPos.above(), Blocks.PALE_OAK_LOG.defaultBlockState(), 3)) {
      return false;
    }

    grownBlocks.add(new PlacedBattleBlock(level, heartPos.above(), CreateBlocks.PALE_OAK_LOG));
    onAnyBlockPlaced(level, heartPos.above());
    return true;
  }

  private void replaceNearbyBlocksWithGold(ServerLevel level, BlockPos centerPos, int radius) {
    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      if (level.getBlockState(targetPos).isAir()) {
        continue;
      }

      replaceWorldBlockWithBattleBlock(
          level,
          targetPos.immutable(),
          Blocks.GOLD_BLOCK.defaultBlockState(),
          CreateBlocks.GOLD_BLOCK);
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

    breakBlocksInPositions(level, positionsToBreak, true);
  }

  private void breakBlocksInPositions(ServerLevel level, Iterable<BlockPos> positions, boolean fromExplosion) {
    for (BlockPos targetPos : positions) {
      breakBattleBlockAt(level, targetPos, fromExplosion);
    }
  }

  private void applyCactusReflection(BattleTeam damagedTeam, BattleTeam attackingTeam, int actualDamageDealt) {
    int cactusCount = countBlocksOnBoard(damagedTeam, BattleBlockIDs.CACTUS);

    if (actualDamageDealt <= 0 || cactusCount <= 0) {
      return;
    }

    int reflectedDamage = (actualDamageDealt / 2) * cactusCount;
    int actualReflectedDamage = shouldIgnoreDefenceForDamage()
        ? attackingTeam.takeDirectHealthDamage(applyBeaconDamageReduction(attackingTeam, adjustDamageAmount(reflectedDamage)))
        : attackingTeam.takeHealthDamage(applyBeaconDamageReduction(attackingTeam, adjustDamageAmount(reflectedDamage)));

    tryReviveIfPossible(attackingTeam);
    damagedTeam.recordLastDamageDealt(actualReflectedDamage);
    applyNetherGoldOreRetaliation(attackingTeam, actualReflectedDamage);
  }

  private boolean isGoldenBlock(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case GOLD_BLOCK, RAW_GOLD_BLOCK, NETHER_GOLD_ORE, DEEPSLATE_GOLD_ORE -> true;
      default -> false;
    };
  }

  private boolean isExplosionProtected(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return false;
    }

    for (Direction direction : Direction.values()) {
      if (isBlock(level, pos.relative(direction), BattleBlockIDs.OBSIDIAN)) {
        return true;
      }
    }

    return false;
  }

  private boolean isProtectedByNetheriteBeacon(ServerLevel level, BlockPos pos) {
    if (level == null || pos == null) {
      return false;
    }

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
        if (placedBlock.level() != level
            || !placedBlock.stillExists()
            || placedBlock.battleBlock().id != BattleBlockIDs.BEACON
            || !isBlock(level, placedBlock.pos().below(), BattleBlockIDs.NETHERITE_BLOCK)) {
          continue;
        }

        if (isWithinCube(placedBlock.pos(), pos, 1)) {
          return true;
        }
      }
    }

    return false;
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

  private boolean isNetherWarpActive() {
    return isWarpActive(BattleWarp.NETHER)
        || isWarpActive(BattleWarp.NETHER_STRIP_MINE)
        || isWarpActive(BattleWarp.NETHER_FORTRESS);
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
      case WITCH_HUT -> battleState.getActiveWitchHutEffect() == WitchHutEffect.EXTRA_DRAW ? 4 : 0;
      default -> 0;
    };
  }

  private int getPlacementsPerTurnForNewTurn() {
    int witchHutPlacements = isWarpActive(BattleWarp.WITCH_HUT)
        && battleState.getActiveWitchHutEffect() == WitchHutEffect.DOUBLE_PLACEMENT
            ? 1
            : 0;

    if (isWarpActive(BattleWarp.BLIZZARD)) {
      return Math.max(
          1,
          battleState.getActiveWarpTurnCount()
              + 1
              + battleState.getActiveTeam().getActiveTurnExtraPlacements()
              + witchHutPlacements);
    }

    return Math.max(1, 1 + battleState.getActiveTeam().getActiveTurnExtraPlacements() + witchHutPlacements);
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
    if (isWarpActive(BattleWarp.WITCH_HUT) && battleState.getActiveWitchHutEffect() == WitchHutEffect.DOUBLE_DAMAGE) {
      amount *= 2;
    }

    if (isWarpActive(BattleWarp.DRIPSTONE_CAVE)) {
      return amount * 2;
    }

    return amount;
  }

  private int adjustHealingAmount(int amount) {
    if (isWarpActive(BattleWarp.NIGHT)) {
      return 0;
    }

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
    int multiplier = 1;

    if (battleBlock != null && isWarpActive(BattleWarp.NIGHT) && isMobHead(battleBlock.id)) {
      multiplier *= 2;
    }

    if (isWarpActive(BattleWarp.BASTION) && level != null && pos != null && battleBlock != null && hasAdjacentGoldenBlock(level, pos)) {
      multiplier *= 2;
    }

    return multiplier;
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
    int healthBefore = team.getHealth();
    team.heal(adjustedAmount);
    emitStatFeedback(team, BattleFeedbackType.HEALING, team.getHealth() - healthBefore);
    refreshCombatPresentation();

    if (isWarpActive(BattleWarp.LUSH_CAVE) && adjustedAmount > 0) {
      dealImmediateDamage(team, enemyTeam, adjustedAmount);
    }
  }

  private void applyImmediateShieldGain(BattleTeam team, int amount) {
    int shieldBefore = team.getShield();
    team.gainShield(adjustShieldGainAmount(amount));
    emitStatFeedback(team, BattleFeedbackType.SHIELD_GAIN, team.getShield() - shieldBefore);
    refreshCombatPresentation();
  }

  private int dealDamageToTeam(BattleTeam team, int amount) {
    if (isDamagePreventedByWitchHut()) {
      return 0;
    }

    int shieldBefore = team.getShield();
    int healthBefore = team.getHealth();
    int adjustedAmount = applyBeaconDamageReduction(team, adjustDamageAmount(amount));
    int actualDamageDealt = shouldIgnoreDefenceForDamage()
        ? team.takeDirectHealthDamage(adjustedAmount)
        : team.takeHealthDamage(adjustedAmount);
    emitStatFeedback(team, BattleFeedbackType.SHIELD_LOSS, shieldBefore - team.getShield());
    emitStatFeedback(team, BattleFeedbackType.DAMAGE, healthBefore - team.getHealth());
    refreshCombatPresentation();

    tryReviveIfPossible(team);
    return actualDamageDealt;
  }

  private void applyStartOfTurnWarpCosts(BattleTeam currentTeam, BattleTeam enemyTeam) {
    if (isWarpActive(BattleWarp.VILLAGE_HOUSE)) {
      applyImmediateHealing(currentTeam, enemyTeam, 16);
      applyImmediateHealing(enemyTeam, currentTeam, 16);
    }

    if (isWarpActive(BattleWarp.DEEP_DARK)) {
      if (currentTeam.getStartingDeck().isEmpty()) {
        dealDamageToTeam(currentTeam, 30);
      } else {
        ServerPlayer player = findOnlinePlayerForTeam(currentTeam.getSide());

        if (player != null) {
          openDeepDarkChoice(player, currentTeam);
        } else {
          dealDamageToTeam(currentTeam, 30);
        }
      }
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

  private void tryReviveIfPossible(BattleTeam team) {
    if (team.getHealth() > 0) {
      return;
    }

    if (tryReviveWithWitchHut(team)
        || tryReviveWithRespawnAnchor(team)
        || tryReviveWithConduit(team)
        || tryReviveWithBed(team)) {
      return;
    }
  }

  private boolean tryReviveWithWitchHut(BattleTeam team) {
    if (team.getHealth() > 0
        || !isWarpActive(BattleWarp.WITCH_HUT)
        || battleState.getActiveWitchHutEffect() != WitchHutEffect.REVIVE_ON_DEATH) {
      return false;
    }

    team.setHealth(10);
    BattleCombatLog.logRevive(resolveServer(), team.getSide(), "Witch Hut", team.getHealth());
    return true;
  }

  private boolean tryReviveWithRespawnAnchor(BattleTeam team) {
    if (team.getHealth() > 0 || !isNetherWarpActive()) {
      return false;
    }

    for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
      if (placedBlock.battleBlock().id != BattleBlockIDs.RESPAWN_ANCHOR
          || !placedBlock.stillExists()
          || countAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.GLOWSTONE) < 2) {
        continue;
      }

      team.setHealth(Math.max(1, (team.getMaxHealth() + 1) / 2));
      BattleCombatLog.logRevive(resolveServer(), team.getSide(), "Respawn Anchor", team.getHealth());
      return true;
    }

    return false;
  }

  private boolean tryReviveWithConduit(BattleTeam team) {
    if (team.getHealth() > 0) {
      return false;
    }

    for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
      if (placedBlock.battleBlock().id != BattleBlockIDs.CONDUIT
          || !placedBlock.stillExists()
          || !hasAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.WATER)
          || !hasAdjacent(placedBlock.level(), placedBlock.pos(), BattleBlockIDs.PRISMARINE)) {
        continue;
      }

      consumePlacedBlock(team, placedBlock);
      team.setHealth(Math.max(1, (team.getMaxHealth() + 1) / 2));
      BattleCombatLog.logRevive(resolveServer(), team.getSide(), "Conduit", team.getHealth());
      return true;
    }

    return false;
  }

  private boolean tryReviveWithBed(BattleTeam team) {
    if (!isWarpActive(BattleWarp.BED_WARS) || team.getHealth() > 0) {
      return false;
    }

    for (PlacedBattleBlock placedBlock : new ArrayList<>(team.getPlacedBlocks())) {
      if (placedBlock.battleBlock().id != BattleBlockIDs.RED_BED || !placedBlock.stillExists()) {
        continue;
      }

      consumePlacedBlock(team, placedBlock);
      team.setHealth(10);
      BattleCombatLog.logRevive(resolveServer(), team.getSide(), "Red Bed", team.getHealth());
      return true;
    }

    return false;
  }

  private void consumePlacedBlock(BattleTeam team, PlacedBattleBlock placedBlock) {
    team.getPlacedBlocks().remove(placedBlock);
    placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
    onAnyBlockBroken(placedBlock.level(), placedBlock.pos());
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
    battleState.setActiveWitchHutEffect(WitchHutEffect.NONE);
    battleState.setActiveWarpStarterSide(starterSide);
    battleState.resetActiveWarpTurnCount();
    battleState.setActiveWarpRoundCount(newWarp == BattleWarp.END && starterSide != null ? 1 : 0);
    BattleCombatLog.logWarpChange(resolveServer(), previousWarp, newWarp);

    if (previousWarp == BattleWarp.FLOWER_FOREST) {
      removeFlowerForestBonus(battleState.getRedTeam());
      removeFlowerForestBonus(battleState.getBlueTeam());
    }

    if (newWarp == BattleWarp.FLOWER_FOREST) {
      applyFlowerForestBonus(battleState.getRedTeam());
      applyFlowerForestBonus(battleState.getBlueTeam());
    }

    applyBoardWideWarpEffects(level);

    if (newWarp == BattleWarp.WITCH_HUT && starterSide != null) {
      rollWitchHutEffectIfNeeded(starterSide);
    }

    if (matchingWarp != null) {
      placeWarpStructure(level, matchingWarp.anchorPos(), matchingWarp.warp());
    }
  }

  private WarpMatch findMatchingWarp(ServerLevel level, BlockPos changedPos) {
    ArrayList<BlockPos> candidatePositions = collectWarpCandidatePositions(level, changedPos);
    boolean suppressNightWarp = battleState.consumeNightWarpSuppression();

    for (BattleWarp warp : BattleWarp.values()) {
      if (warp == BattleWarp.NONE || warp == BattleWarp.SWAMP || (suppressNightWarp && warp == BattleWarp.NIGHT)) {
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
      case NIGHT -> matchesNightWarp(level, pos);
      case TRIAL_CHAMBER -> matchesTrialChamberWarp(level, pos);
      case VILLAGE_HOUSE -> matchesVillageHouseWarp(level, pos);
      case END -> matchesEndWarp(level, pos);
      case LIBRARY -> matchesLibraryWarp(level, pos);
      case WITCH_HUT -> matchesWitchHutWarp(level, pos);
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

  private boolean matchesTrialChamberWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.VAULT)) {
      return isBlock(level, pos.below(), BattleBlockIDs.COPPER_BLOCK);
    }

    return isBlock(level, pos, BattleBlockIDs.COPPER_BLOCK)
        && isBlock(level, pos.above(), BattleBlockIDs.VAULT);
  }

  private boolean matchesNightWarp(ServerLevel level, BlockPos pos) {
    BattleBlockIDs blockId = getBattleBlockId(level, pos);

    if (blockId == null) {
      return false;
    }

    if (blockId == BattleBlockIDs.CREEPER_HEAD) {
      return (hasAdjacent(level, pos, BattleBlockIDs.GRASS_BLOCK) && hasAdjacent(level, pos, BattleBlockIDs.ZOMBIE_HEAD, BattleBlockIDs.SKELETON_SKULL))
          || (hasAdjacent(level, pos, BattleBlockIDs.ZOMBIE_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.SKELETON_SKULL));
    }

    if (blockId == BattleBlockIDs.ZOMBIE_HEAD) {
      return (hasAdjacent(level, pos, BattleBlockIDs.GRASS_BLOCK) && hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD, BattleBlockIDs.SKELETON_SKULL))
          || (hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.SKELETON_SKULL));
    }

    if (blockId == BattleBlockIDs.SKELETON_SKULL) {
      return (hasAdjacent(level, pos, BattleBlockIDs.GRASS_BLOCK) && hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD, BattleBlockIDs.ZOMBIE_HEAD))
          || (hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.ZOMBIE_HEAD));
    }

    return blockId == BattleBlockIDs.GRASS_BLOCK
        && ((hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.ZOMBIE_HEAD))
            || (hasAdjacent(level, pos, BattleBlockIDs.CREEPER_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.SKELETON_SKULL))
            || (hasAdjacent(level, pos, BattleBlockIDs.ZOMBIE_HEAD) && hasAdjacent(level, pos, BattleBlockIDs.SKELETON_SKULL)));
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

  private boolean matchesWitchHutWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.BREWING_STAND)) {
      return hasHorizontalAdjacent(level, pos, BattleBlockIDs.CAULDRON)
          || hasHorizontalAdjacent(level, pos, BattleBlockIDs.OAK_PLANKS);
    }

    if (isBlock(level, pos, BattleBlockIDs.CAULDRON)) {
      return hasHorizontalAdjacent(level, pos, BattleBlockIDs.BREWING_STAND)
          || hasHorizontalAdjacent(level, pos, BattleBlockIDs.OAK_PLANKS);
    }

    return isBlock(level, pos, BattleBlockIDs.OAK_PLANKS)
        && (hasHorizontalAdjacent(level, pos, BattleBlockIDs.CAULDRON)
            || hasHorizontalAdjacent(level, pos, BattleBlockIDs.BREWING_STAND));
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
    if (isBlock(level, pos, BattleBlockIDs.NETHER_GOLD_ORE)) {
      return hasAdjacent(level, pos, BattleBlockIDs.NETHER_QUARTZ_ORE, BattleBlockIDs.ANCIENT_DEBRIS);
    }

    if (isAnyBlock(level, pos, BattleBlockIDs.NETHER_QUARTZ_ORE, BattleBlockIDs.ANCIENT_DEBRIS)) {
      return hasAdjacent(level, pos, BattleBlockIDs.NETHER_GOLD_ORE, BattleBlockIDs.NETHER_QUARTZ_ORE, BattleBlockIDs.ANCIENT_DEBRIS);
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

    return false;
  }

  private boolean matchesStripMineWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.DEEPSLATE)) {
      return hasAdjacent(level, pos, BattleBlockIDs.DEEPSLATE_GOLD_ORE, BattleBlockIDs.DEEPSLATE_REDSTONE_ORE);
    }

    if (isAnyBlock(level, pos, BattleBlockIDs.DEEPSLATE_GOLD_ORE, BattleBlockIDs.DEEPSLATE_REDSTONE_ORE)) {
      return hasAdjacent(
          level,
          pos,
          BattleBlockIDs.DEEPSLATE,
          BattleBlockIDs.DEEPSLATE_GOLD_ORE,
          BattleBlockIDs.DEEPSLATE_REDSTONE_ORE);
    }

    return false;
  }

  private boolean matchesBedWarsWarp(ServerLevel level, BlockPos pos) {
    if (isBlock(level, pos, BattleBlockIDs.RED_BED)) {
      return countHorizontalRingBlocks(level, pos, BattleBlockIDs.OAK_PLANKS) >= 8
          || countAdjacentAndAbove(level, pos, BattleBlockIDs.END_STONE) >= 2;
    }

    if (isBlock(level, pos, BattleBlockIDs.OAK_PLANKS)) {
      return hasAdjacent(level, pos, BattleBlockIDs.END_STONE)
          && countAdjacent(level, pos, BattleBlockIDs.OAK_PLANKS) >= 3;
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

    if (centerBlockId == BattleBlockIDs.RED_SAND) {
      return hasAdjacent(level, pos, BattleBlockIDs.RED_SANDSTONE);
    }

    return false;
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
    changeMaxHealth(team, 70);
    healWithoutWarp(team, 70);
  }

  private void removeFlowerForestBonus(BattleTeam team) {
    damageTeamDirectly(team, 70);
    changeMaxHealth(team, -70);
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

  private boolean hasAdjacentWater(ServerLevel level, BlockPos pos) {
    return hasAdjacent(level, pos, BattleBlockIDs.WATER);
  }

  private boolean hasAdjacentBlockId(ServerLevel level, BlockPos pos, String blockId) {
    for (Direction direction : Direction.values()) {
      if (getBlockId(level, pos.relative(direction)).equals(blockId)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasNearbyWorldBlock(ServerLevel level, BlockPos centerPos, int radius, BattleBlockIDs... battleBlockIds) {
    for (BlockPos targetPos : BlockPos.betweenClosed(
        centerPos.offset(-radius, -radius, -radius),
        centerPos.offset(radius, radius, radius))) {
      if (targetPos.equals(centerPos)) {
        continue;
      }

      if (isAnyBlock(level, targetPos.immutable(), battleBlockIds)) {
        return true;
      }
    }

    return false;
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

  private void changeMaxHealth(BattleTeam team, int amount) {
    if (amount == 0) {
      return;
    }

    int maxHealthBefore = team.getMaxHealth();
    team.increaseMaxHealth(amount);
    emitStatFeedback(team, BattleFeedbackType.MAX_HEALTH, team.getMaxHealth() - maxHealthBefore);
    refreshCombatPresentation();
  }

  private void healWithoutWarp(BattleTeam team, int amount) {
    if (amount <= 0) {
      return;
    }

    int healthBefore = team.getHealth();
    team.heal(amount);
    emitStatFeedback(team, BattleFeedbackType.HEALING, team.getHealth() - healthBefore);
    refreshCombatPresentation();
  }

  private void damageTeamDirectly(BattleTeam team, int amount) {
    if (amount <= 0) {
      return;
    }

    int healthBefore = team.getHealth();
    team.setHealth(team.getHealth() - amount);
    emitStatFeedback(team, BattleFeedbackType.DAMAGE, healthBefore - team.getHealth());
    refreshCombatPresentation();
  }

  private void refreshCombatPresentation() {
    BattleFeedback.refreshCombatPresentation(resolveServer(), battleState);
  }

  private void emitStatFeedback(BattleTeam team, BattleFeedbackType type, int amount) {
    BattleFeedback.emitStatFeedback(resolveServer(), team, type, amount);
  }

  private void emitBlockActivation(
      BattleTeam sourceTeam,
      ServerLevel level,
      BlockPos pos,
      BattleFeedbackType type,
      int amount) {
    BattleFeedback.emitBlockActivation(resolveServer(), sourceTeam, level, pos, type, amount);
  }

  private MinecraftServer resolveServer() {
    return BattleFeedback.resolveServer(battleState, lastKnownServer);
  }

  private record WarpMatch(BattleWarp warp, BlockPos anchorPos) {
  }

  private record PlacedBlockResolution(BattleBlock boardBlock, BattleBlock effectBlock, List<BattleBlock> storedBlocks) {
  }

  private record OwnedPlacedBattleBlock(BattleTeam ownerTeam, PlacedBattleBlock placedBlock) {
  }
}
