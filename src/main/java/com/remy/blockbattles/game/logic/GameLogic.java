package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.Iterator;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.BattleBlockIDs;
import com.remy.blockbattles.game.blocks.Classification;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.blocks.abilities.Abilities;
import com.remy.blockbattles.game.gui.BattleCardItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class GameLogic {
  private final BattleState battleState;
  private final DeckManager deckManager;

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

  public boolean canPlaceBattleBlock(String blockId, TeamSide actingSide) {
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
      case CHERRY_LEAVES -> {
        queueHealingSource(currentTeam, currentTeam.getMaxHealth() / 10, true);
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
    if (!canPlaceBattleBlock(battleBlock.id.getId(), actingSide)) {
      return false;
    }

    TeamSide resolvedActingSide = resolveActingSide(actingSide);
    BattleTeam currentTeam = getTeamForTurn(resolvedActingSide);
    BattleTeam enemyTeam = getEnemyTeamForTurn(resolvedActingSide);
    BattleBlock placedBlock = applyOnPlaceAbility(battleBlock, currentTeam, enemyTeam, level, pos);

    queueImmediateBlockEffects(battleBlock, currentTeam);

    endTurn(resolvedActingSide);

    if (level != null && pos != null && placedBlock != null) {
      registerPlacedBlock(placedBlock, level, pos, resolvedActingSide);
    }

    return true;
  }

  private void queueImmediateBlockEffects(BattleBlock battleBlock, BattleTeam currentTeam) {
    if (!battleBlock.damagePerTurn && battleBlock.damage != 0) {
      if (battleBlock.id == BattleBlockIDs.SOUL_LANTERN) {
        queueDirectHealthDamageSource(currentTeam, battleBlock.damage, false);
      } else {
        queueDamageSource(currentTeam, battleBlock.damage, false);
      }
    }

    if (!battleBlock.healingPerTurn && battleBlock.healing != 0) {
      queueHealingSource(currentTeam, battleBlock.healing, false);
    }

    if (!battleBlock.defencePerTurn && battleBlock.defence != 0) {
      queueShieldSource(currentTeam, battleBlock.defence, false);
    }

    if (!battleBlock.defenceDamagePerTurn) {
      pendingShieldDamage += battleBlock.defenceDamage;
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
    Iterator<PlacedBattleBlock> iterator = currentTeam.getPlacedBlocks().iterator();
    ArrayList<PlacedBattleBlock> grownBlocks = new ArrayList<>();

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!placedBlock.stillExists()) {
        handleBrokenPlacedBlock(currentTeam, enemyTeam, placedBlock);
        iterator.remove();
        continue;
      }

      BattleBlock battleBlock = placedBlock.battleBlock();

      if (battleBlock.damagePerTurn) {
        queueDamageSource(currentTeam, getPerTurnDamageAmount(placedBlock, currentTeam), false);
      }

      if (battleBlock.healingPerTurn) {
        queueHealingSource(currentTeam, getPerTurnHealingAmount(placedBlock), false);
      }

      if (battleBlock.defencePerTurn) {
        queueShieldSource(currentTeam, getPerTurnShieldAmount(placedBlock), false);
      }

      if (battleBlock.defenceDamagePerTurn) {
        pendingShieldDamage += battleBlock.defenceDamage;
      }

      applyPerTurnAbility(currentTeam, enemyTeam, placedBlock, iterator, grownBlocks);
    }

    grownBlocks.forEach(currentTeam::addPlacedBlock);
  }

  private void applyPerTurnAbility(
      BattleTeam currentTeam,
      BattleTeam enemyTeam,
      PlacedBattleBlock placedBlock,
      Iterator<PlacedBattleBlock> iterator,
      ArrayList<PlacedBattleBlock> grownBlocks) {
    switch (placedBlock.battleBlock().id) {
      case CHERRY_LOG, JUNGLE_LOG -> {
        if (Abilities.growBlockUpwardIfAir(placedBlock.level(), placedBlock.pos())) {
          grownBlocks.add(new PlacedBattleBlock(
              placedBlock.level(),
              placedBlock.pos().above(),
              placedBlock.battleBlock()));
        }
      }
      case DRAGON_EGG -> {
        if (placedBlock.advanceOwnerTurnCount() >= 2) {
          queueHealingSource(currentTeam, 32, true);
          iterator.remove();
          placedBlock.level().setBlock(placedBlock.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
      }
      default -> {
      }
    }
  }

  private void queueQueuedTurnEffects(BattleTeam currentTeam) {
    queueDamageSource(currentTeam, currentTeam.consumeActiveTurnDamage(), false);
    queueHealingSource(currentTeam, currentTeam.consumeActiveTurnHealing(), false);
    queueShieldSource(currentTeam, currentTeam.consumeActiveTurnShieldGain(), false);
  }

  private int countBlocksOnBoard(Classification classification, BattleBlock additionalBlock) {
    int count = countBlocksOnBoard(battleState.getRedTeam(), classification)
        + countBlocksOnBoard(battleState.getBlueTeam(), classification);

    if (additionalBlock != null && additionalBlock.classification == classification) {
      count++;
    }

    return count;
  }

  private int countBlocksOnBoard(BattleTeam team, Classification classification) {
    int count = 0;
    Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!placedBlock.stillExists()) {
        handleBrokenPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock);
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
    applyNetherGoldOreRetaliation(enemyTeam, actualDamageDealt);

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
    battleState.getActiveTeam().activateQueuedTurnEffects();
    drawHandForTeam(battleState.getActiveTeam());
  }

  public void updateConfiguredDeck(TeamSide side, java.util.List<BattleBlock> deck) {
    battleState.setConfiguredDeck(side, deck);
    battleState.applyConfiguredDeck(side);

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

  public void forceTurn(TeamSide side) {
    battleState.getRedTeam().clearHand();
    battleState.getBlueTeam().clearHand();
    battleState.getRedTeam().clearActiveTurnEffects();
    battleState.getBlueTeam().clearActiveTurnEffects();
    battleState.setActiveSide(side);
    battleState.getActiveTeam().activateQueuedTurnEffects();
    drawHandForTeam(battleState.getActiveTeam());
  }

  public void redrawActiveHand() {
    drawHandForTeam(battleState.getActiveTeam());
  }

  private void drawHandForTeam(BattleTeam team) {
    team.clearHand();
    deckManager.dealCards(team, Math.max(0,
        BattleCardItems.HAND_SIZE
            + team.getActiveTurnDrawModifier()
            + countBlocksOnBoard(team, BattleBlockIDs.SHULKER_BOX)));
  }

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingDirectHealthDamage = 0;
    pendingHealing = 0;
    pendingMaxHealthGain = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }

  int getPerTurnDamageAmount(PlacedBattleBlock placedBlock, BattleTeam currentTeam) {
    return applyCarpetModifier(
        placedBlock,
        getPerTurnDamageAmount(placedBlock.battleBlock(), currentTeam),
        BattleBlockIDs.RED_CARPET);
  }

  int getPerTurnDamageAmount(BattleBlock battleBlock, BattleTeam currentTeam) {
    int damageAmount = battleBlock.damage;

    if (battleBlock.id == BattleBlockIDs.DEAD_BUSH) {
      damageAmount += currentTeam.getMissingHealth() / 5;
    }

    return damageAmount;
  }

  private int getPerTurnHealingAmount(PlacedBattleBlock placedBlock) {
    return applyCarpetModifier(placedBlock, placedBlock.battleBlock().healing, BattleBlockIDs.GREEN_CARPET);
  }

  private int getPerTurnShieldAmount(PlacedBattleBlock placedBlock) {
    return applyCarpetModifier(placedBlock, placedBlock.battleBlock().defence, BattleBlockIDs.BLUE_CARPET);
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
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);
    return trackedBlock == null || trackedBlock.placedBlock().battleBlock().id != BattleBlockIDs.ANCIENT_DEBRIS;
  }

  public void onBattleBlockBroken(ServerLevel level, BlockPos pos) {
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
    breakBattleBlockAt(level, pos.north());
    breakBattleBlockAt(level, pos.south());
    breakBattleBlockAt(level, pos.east());
    breakBattleBlockAt(level, pos.west());
    breakBattleBlockAt(level, pos);
  }

  private void breakBattleBlockAt(ServerLevel level, BlockPos pos) {
    OwnedPlacedBattleBlock trackedBlock = findTrackedBlock(level, pos);

    if (trackedBlock != null && trackedBlock.placedBlock().battleBlock().id == BattleBlockIDs.ANCIENT_DEBRIS) {
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

        if (!placedBlock.stillExists()) {
          handleBrokenPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock);
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

  private void dealImmediateDamage(BattleTeam attackingTeam, BattleTeam defendingTeam, int amount) {
    int actualDamageDealt = defendingTeam.takeHealthDamage(amount);
    attackingTeam.recordLastDamageDealt(actualDamageDealt);
    applyNetherGoldOreRetaliation(defendingTeam, actualDamageDealt);
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

      if (!placedBlock.stillExists()) {
        handleBrokenPlacedBlock(team, battleState.getOpponentOf(team.getSide()), placedBlock);
        iterator.remove();
        continue;
      }

      if (placedBlock.battleBlock().id == battleBlockId) {
        count++;
      }
    }

    return count;
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
