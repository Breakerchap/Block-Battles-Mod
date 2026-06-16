package com.remy.blockbattles.game.logic;

import java.util.Iterator;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.BattleBlockIDs;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.blocks.abilities.Abilities;
import com.remy.blockbattles.game.gui.BattleCardItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class GameLogic {
  private final BattleState battleState;
  private final DeckManager deckManager;

  private int pendingDamage;
  private int pendingHealing;
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
        .map(this::onPlaceBattleBlock)
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos) {
    return onPlaceBattleBlock(blockId, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos, TeamSide actingSide) {
    if (!canPlaceBattleBlock(blockId, actingSide)) {
      return false;
    }

    boolean wasTnt = blockId.equals(BattleBlockIDs.TNT.getId());
    boolean wasRedTulip = blockId.equals(BattleBlockIDs.RED_TULIP.getId());

    // @formatter:off
    if (wasTnt) {Abilities.tntAbility(level, pos, actingSide);}
    else if (wasRedTulip) {Abilities.redTulipAbility(getTeamForTurn(actingSide));} 
    // @formatter:on

    boolean wasBattleBlock = CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> {
          if (level instanceof ServerLevel serverLevel) {
            return onPlaceBattleBlock(battleBlock, serverLevel, pos, actingSide);
          }

          return onPlaceBattleBlock(battleBlock, actingSide);
        })
        .orElse(false);

    return wasTnt || wasBattleBlock;
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock) {
    return onPlaceBattleBlock(battleBlock, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, TeamSide actingSide) {
    if (!canPlaceBattleBlock(battleBlock.id.getId(), actingSide)) {
      return false;
    }

    queueImmediateBlockEffects(battleBlock);

    endTurn(actingSide);
    return true;
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    return onPlaceBattleBlock(battleBlock, level, pos, battleState.getActiveSide());
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    registerPlacedBlock(battleBlock, level, pos, actingSide);
    return onPlaceBattleBlock(battleBlock, actingSide);
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

  private void queueImmediateBlockEffects(BattleBlock battleBlock) {
    if (!battleBlock.damagePerTurn) {
      pendingDamage += battleBlock.damage;
    }

    if (!battleBlock.healingPerTurn) {
      pendingHealing += battleBlock.healing;
    }

    if (!battleBlock.defencePerTurn) {
      pendingShieldGain += battleBlock.defence;
    }

    if (!battleBlock.defenceDamagePerTurn) {
      pendingShieldDamage += battleBlock.defenceDamage;
    }
  }

  private void registerPlacedBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos, TeamSide actingSide) {
    if (!hasPerTurnEffect(battleBlock)) {
      return;
    }

    getTeamForTurn(actingSide).addPlacedBlock(new PlacedBattleBlock(level, pos, battleBlock));
  }

  private boolean hasPerTurnEffect(BattleBlock battleBlock) {
    return battleBlock.damagePerTurn
        || battleBlock.healingPerTurn
        || battleBlock.defencePerTurn
        || battleBlock.defenceDamagePerTurn;
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

  private void queuePerTurnEffects(BattleTeam team) {
    Iterator<PlacedBattleBlock> iterator = team.getPlacedBlocks().iterator();

    while (iterator.hasNext()) {
      PlacedBattleBlock placedBlock = iterator.next();

      if (!placedBlock.stillExists()) {
        iterator.remove();
        continue;
      }

      BattleBlock battleBlock = placedBlock.battleBlock();

      if (battleBlock.damagePerTurn) {
        pendingDamage += battleBlock.damage;
      }

      if (battleBlock.healingPerTurn) {
        pendingHealing += battleBlock.healing;
      }

      if (battleBlock.defencePerTurn) {
        pendingShieldGain += battleBlock.defence;
      }

      if (battleBlock.defenceDamagePerTurn) {
        pendingShieldDamage += battleBlock.defenceDamage;
      }
    }
  }

  public void endTurn() {
    endTurn(battleState.getActiveSide());
  }

  public void endTurn(TeamSide actingSide) {
    TeamSide resolvedActingSide = resolveActingSide(actingSide);
    BattleTeam currentTeam = getTeamForTurn(resolvedActingSide);
    BattleTeam enemyTeam = getEnemyTeamForTurn(resolvedActingSide);

    queuePerTurnEffects(currentTeam);

    currentTeam.heal(pendingHealing);
    currentTeam.gainShield(pendingShieldGain);

    enemyTeam.takeHealthDamage(pendingDamage);
    enemyTeam.loseShield(pendingShieldDamage);

    clearPendingEffects();
    currentTeam.clearHand();
    battleState.setActiveSide(resolvedActingSide.otherSide());
    drawHandForTeam(battleState.getActiveTeam());
  }

  public void resetBattle() {
    clearPendingEffects();
    battleState.clearPlacedBlocks();
    battleState.resetForNewBattle();
    battleState.getRedTeam().clearHand();
    battleState.getBlueTeam().clearHand();
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

  private void drawHandForTeam(BattleTeam team) {
    team.clearHand();
    deckManager.dealCards(team, BattleCardItems.HAND_SIZE);
  }

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingHealing = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }
}
