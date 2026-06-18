package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Blocks;
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

  public boolean canPlaceBattleBlock(String blockId, TeamSide actingSide) {
    if (!battleState.isGameRunning()) {
      return true;
    }

    if (!isBattlePlacement(blockId)) {
      return true;
    }

    return actingSide != null
        && actingSide == battleState.getActiveSide()
        && battleState.getTeam(actingSide).hasCardInHand(blockId);
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

    switch (battleBlock.id) {
      case TNT -> {
        if (level != null && pos != null) {
          breakBlocksAround(level, pos);
        }

        return null;
      }
      case RED_TULIP -> {
        Abilities.redTulipAbility(currentTeam);
      }
      case CORNFLOWER -> {
        queueHealingSource(currentTeam, currentTeam.getHealth() / 4, true);
      }
      case PINK_PETALS -> currentTeam.queueHealingAlsoIncreasesMaxHealth();
      case CARVED_PUMPKIN -> {
        if (level != null && pos != null) {
          queueHealingSource(currentTeam, countNearbyBlocks(level, pos, 1) * 4, true);
        }
      }
      case CHERRY_LEAVES -> {
        queueHealingSource(currentTeam, currentTeam.getMaxHealth() / 10, true);
      }
      case MOSS_BLOCK -> {
        if (level != null && pos != null) {
          replaceNearbyBlocksWithGrass(level, pos, 1);
        }
      }
      case NETHERRACK -> {
        queueDamageSource(currentTeam, currentTeam.getLastDamageDealt(), true);
      }
      case VAULT -> {
        if (currentTeam.removeOneCardFromDeck(CreateBlocks.VAULT)) {
          queueShieldSource(currentTeam, 8, true);
          queueHealingSource(currentTeam, 8, true);
        }
      }
      case SCULK -> enemyTeam.increaseMaxHealth(-2);
      case END_CRYSTAL -> {
        if (level != null && pos != null) {
          breakBlocksInCube(level, pos, 2, true);
        }

        return null;
      }
      case FURNACE -> {
        if (level != null && pos != null) {
          queueShieldSource(currentTeam, countNearbyBlocks(level, pos, 1), true);
        }
      }
      case LAPIS_BLOCK -> {
        queueHealingSource(currentTeam, (currentTeam.getHealth() + 1) / 2, true);
        currentTeam.replaceOneCardInDeck(CreateBlocks.LAPIS_BLOCK, CreateBlocks.DIRT);

        if (level != null && pos != null) {
          level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        }

        return CreateBlocks.DIRT;
      }
      case DEEPSLATE_BRICKS -> {
        queueDamageSource(currentTeam, currentTeam.getShield() * 2, true);
      }
      case REINFORCED_DEEPSLATE -> {
        queueShieldSource(currentTeam, Math.min(currentTeam.getShield(), 10), true);
      }
      case WITHER_SKELETON_SKULL -> {
        if (enemyTeam.getShield() > 6) {
          queueDamageSource(currentTeam, 8, true);
        }
      }
      case WITHER_ROSE -> {
        if (level != null && pos != null) {
          int replacedGrassBlocks = replaceNearbyGrassWithSoulSand(level, pos, 1);

          if (replacedGrassBlocks > 0) {
            queueDamageSource(currentTeam, replacedGrassBlocks * 3, true);
            queueHealingSource(currentTeam, replacedGrassBlocks, true);
          }
        }
      }
      case LECTERN -> {
        queueDamageSource(currentTeam, currentTeam.getHandSize(), true);
      }
      case SMITHING_TABLE -> {
        queueDamageSource(currentTeam, countBlocksOnBoard(Classification.MAN_MADE, battleBlock), true);
      }
      case SOUL_TORCH -> currentTeam.queueTurnHealingBonus(4);
      case REDSTONE_TORCH -> currentTeam.queueTurnDamageBonus(5);
      case TORCH -> currentTeam.queueTurnShieldBonus(2);
      case COPPER_TORCH -> currentTeam.queueIgnoreNextIncomingDamage();
      case CRIMSON_HYPHAE -> {
        queueDamageSource(currentTeam, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock), true);
      }
      case WARPED_HYPHAE -> {
        queueHealingSource(currentTeam, countBlocksOnBoard(Classification.OTHERWORLDLY, battleBlock), true);
      }
      case RAW_IRON_BLOCK -> currentTeam.queueTurnDamage(30);
      case RAW_GOLD_BLOCK -> currentTeam.queueTurnShieldGain(6);
      case RAW_COPPER_BLOCK -> currentTeam.queueTurnHealing(26);
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
          queueDamageSource(currentTeam, 8, true);
        }
      }
      case SOUL_SAND -> enemyTeam.queueTurnDrawModifier(-1);
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
    BattleBlock placedBlock = applyOnPlaceAbility(battleBlock, currentTeam, enemyTeam, level, pos);

    queueImmediateBlockEffects(battleBlock, currentTeam, level, pos);

    endTurn(resolvedActingSide);

    if (level != null && pos != null && placedBlock != null) {
      registerPlacedBlock(placedBlock, level, pos, resolvedActingSide);
    }

    return true;
  }

  private void queueImmediateBlockEffects(BattleBlock battleBlock, BattleTeam currentTeam, ServerLevel level, BlockPos pos) {
    if (level != null && pos != null && isActivationSuppressed(battleBlock, level, pos)) {
      return;
    }

    int statBonus = getPodzolStatBonus(currentTeam, level, pos);
    int damage = battleBlock.damage + statBonus;
    int healing = battleBlock.healing + statBonus;
    int shield = battleBlock.defence + statBonus;
    int shieldDamage = battleBlock.defenceDamage + statBonus;

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
      pendingShieldDamage += shieldDamage;
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
      pendingDamage += amount + currentTeam.getActiveTurnDamageBonus();
    }
  }

  private void queueDirectHealthDamageSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      pendingDirectHealthDamage += amount + currentTeam.getActiveTurnDamageBonus();
    }
  }

  private void queueHealingSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      int totalHealing = amount + currentTeam.getActiveTurnHealingBonus();
      pendingHealing += totalHealing;

      if (currentTeam.isActiveHealingAlsoIncreasesMaxHealth() && totalHealing > 0) {
        pendingMaxHealthGain += totalHealing;
      }
    }
  }

  private void queueShieldSource(BattleTeam currentTeam, int amount, boolean applyBonusEvenWhenZero) {
    if (amount != 0 || applyBonusEvenWhenZero) {
      pendingShieldGain += amount + currentTeam.getActiveTurnShieldBonus();
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
        pendingShieldDamage += getPerTurnShieldDamageAmount(placedBlock, currentTeam);
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
        currentTeam.takeHealthDamage(2);
      }
      case 1 -> currentTeam.heal(currentTeam.getLastDamageTakenFromOpponent() / 4);
      case 2 -> currentTeam.gainShield(6);
      case 3 -> currentTeam.queueTurnDrawModifier(2);
      case 4 -> enemyTeam.queueTurnDrawModifier(-1);
      case 5 -> {
        breakBlocksInCube(placedBlock.level(), placedBlock.pos(), 1, true);
        dealImmediateDamage(currentTeam, enemyTeam, 5);
        currentTeam.takeHealthDamage(5);
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

    return switch (battleBlock.id) {
      case DAYLIGHT_DETECTOR -> isInSunlight(placedBlock.level(), placedBlock.pos());
      case SPAWNER -> !isInSunlight(placedBlock.level(), placedBlock.pos());
      default -> true;
    };
  }

  private boolean isActivationSuppressed(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    return isMobHeadOrSpawner(battleBlock.id) && hasNearbyBlock(level, pos, 2, BattleBlockIDs.CANDLE);
  }

  private boolean isMobHeadOrSpawner(BattleBlockIDs battleBlockId) {
    return switch (battleBlockId) {
      case SPAWNER,
          CREEPER_HEAD,
          PIGLIN_HEAD,
          SKELETON_SKULL,
          WITHER_SKELETON_SKULL,
          ZOMBIE_HEAD,
          DRAGON_HEAD -> true;
      default -> false;
    };
  }

  private boolean isInSunlight(ServerLevel level, BlockPos pos) {
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
    return applyCarpetModifier(
        placedBlock,
        placedBlock.battleBlock().damage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos()),
        BattleBlockIDs.RED_CARPET);
  }

  private int getImmediateHealingAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return applyCarpetModifier(
        placedBlock,
        placedBlock.battleBlock().healing + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos()),
        BattleBlockIDs.GREEN_CARPET);
  }

  private int getImmediateShieldAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return applyCarpetModifier(
        placedBlock,
        placedBlock.battleBlock().defence + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos()),
        BattleBlockIDs.BLUE_CARPET);
  }

  private int getImmediateShieldDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return placedBlock.battleBlock().defenceDamage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
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
      pendingShieldDamage += getPerTurnShieldDamageAmount(placedBlock, ownerTeam);
    } else {
      int shieldDamageAmount = getImmediateShieldDamageAmount(placedBlock, ownerTeam);

      if (shieldDamageAmount != 0) {
        pendingShieldDamage += shieldDamageAmount;
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

    queuePerTurnEffects(currentTeam, enemyTeam);
    queueQueuedTurnEffects(currentTeam);

    if (pendingMaxHealthGain > 0) {
      currentTeam.increaseMaxHealth(pendingMaxHealthGain);
    }

    currentTeam.heal(pendingHealing);
    currentTeam.gainShield(pendingShieldGain);

    int actualDamageDealt = 0;

    if (enemyTeam.consumeIgnoreNextIncomingDamage()) {
      pendingDamage = 0;
      pendingDirectHealthDamage = 0;
    }

    actualDamageDealt += enemyTeam.takeHealthDamage(pendingDamage);
    actualDamageDealt += enemyTeam.takeDirectHealthDamage(pendingDirectHealthDamage);
    enemyTeam.loseShield(pendingShieldDamage);
    currentTeam.recordLastDamageDealt(actualDamageDealt);
    enemyTeam.recordLastDamageTakenFromOpponent(actualDamageDealt);
    applyNetherGoldOreRetaliation(enemyTeam, actualDamageDealt);
    applyCactusReflection(enemyTeam, currentTeam, actualDamageDealt);

    currentTeam.clearActiveTurnEffects();
    clearPendingEffects();
    currentTeam.clearHand();
    battleState.setActiveSide(resolvedActingSide.otherSide());
    battleState.getActiveTeam().activateQueuedTurnEffects();
    drawHandForTeam(battleState.getActiveTeam());
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

    battleState.getActiveTeam().activateQueuedTurnEffects();
    drawHandForTeam(battleState.getActiveTeam());
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
    battleState.getActiveTeam().activateQueuedTurnEffects();
    drawHandForTeam(battleState.getActiveTeam());
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

  private void drawHandForTeam(BattleTeam team) {
    team.clearHand();
    deckManager.dealCards(team, Math.max(0,
        BattleCardItems.HAND_SIZE
            + team.getActiveTurnDrawModifier()
            + countBlocksOnBoard(team, BattleBlockIDs.SHULKER_BOX)
            - countBlocksOnBoard(battleState.getOpponentOf(team.getSide()), BattleBlockIDs.SNOW)));
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
    int damageAmount = getPerTurnDamageAmount(placedBlock.battleBlock(), currentTeam)
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.CRIMSON_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      damageAmount += 3;
    }

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
    int healingAmount = placedBlock.battleBlock().healing
        + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());

    if (placedBlock.battleBlock().id == BattleBlockIDs.WARPED_NYLIUM
        && countExistingBlocksOnBoard(Classification.OTHERWORLDLY) >= 6) {
      healingAmount += 3;
    }

    return applyCarpetModifier(placedBlock, healingAmount, BattleBlockIDs.GREEN_CARPET);
  }

  private int getPerTurnShieldAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return applyCarpetModifier(
        placedBlock,
        placedBlock.battleBlock().defence + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos()),
        BattleBlockIDs.BLUE_CARPET);
  }

  private int getPerTurnShieldDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return placedBlock.battleBlock().defenceDamage + getPodzolStatBonus(currentTeam, placedBlock.level(), placedBlock.pos());
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
    return trackedBlock == null || !isUnbreakableBattleBlock(trackedBlock.placedBlock().battleBlock().id);
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
        team.gainShield(4);
        team.heal(10);
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
    int actualDamageDealt = defendingTeam.takeHealthDamage(amount);
    attackingTeam.recordLastDamageDealt(actualDamageDealt);
    defendingTeam.recordLastDamageTakenFromOpponent(actualDamageDealt);
    applyNetherGoldOreRetaliation(defendingTeam, actualDamageDealt);
    applyCactusReflection(defendingTeam, attackingTeam, actualDamageDealt);
  }

  private void applyNetherGoldOreRetaliation(BattleTeam damagedTeam, int actualDamageDealt) {
    if (actualDamageDealt <= 0) {
      return;
    }

    damagedTeam.gainShield(countBlocksOnBoard(damagedTeam, BattleBlockIDs.NETHER_GOLD_ORE));
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
  }

  public void onAnyBlockBroken(ServerLevel level, BlockPos pos) {
    if (!battleState.isGameRunning()) {
      return;
    }

    triggerSculkSensors(level, pos);
    triggerSculkCatalysts(level, pos);
    triggerSculkShriekers(level, pos);
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
        ownerTeam.takeHealthDamage(blocksFallen * 5);
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
    int actualReflectedDamage = attackingTeam.takeHealthDamage(reflectedDamage);

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

  private record OwnedPlacedBattleBlock(BattleTeam ownerTeam, PlacedBattleBlock placedBlock) {
  }
}
