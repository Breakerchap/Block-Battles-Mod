package com.remy.blockbattles.game.logic;

import java.util.Iterator;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.BattleBlockIDs;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.blocks.abilities.Abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class GameLogic {
  private final BattleState battleState;

  private int pendingDamage;
  private int pendingHealing;
  private int pendingShieldGain;
  private int pendingShieldDamage;

  public GameLogic() {
    this(BattleState.shared());
  }

  public GameLogic(BattleState battleState) {
    this.battleState = battleState;
  }

  public boolean onPlaceBattleBlock(String blockId) {
    return CreateBlocks.findByMinecraftId(blockId)
        .map(this::onPlaceBattleBlock)
        .orElse(false);
  }

  public boolean onPlaceBattleBlock(String blockId, Level level, BlockPos pos) {
    boolean wasTnt = blockId.equals(BattleBlockIDs.TNT.getId());

    if (wasTnt) {
      Abilities.tntAbility(level, pos);
    }

    boolean wasBattleBlock = CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> {
          if (level instanceof ServerLevel serverLevel) {
            return onPlaceBattleBlock(battleBlock, serverLevel, pos);
          }

          return onPlaceBattleBlock(battleBlock);
        })
        .orElse(false);

    return wasTnt || wasBattleBlock;
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock) {
    queueImmediateBlockEffects(battleBlock);

    endTurn();
    return true;
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    registerPlacedBlock(battleBlock, level, pos);
    return onPlaceBattleBlock(battleBlock);
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

  private void registerPlacedBlock(BattleBlock battleBlock, ServerLevel level, BlockPos pos) {
    if (!hasPerTurnEffect(battleBlock)) {
      return;
    }

    getCurrentTeam().addPlacedBlock(new PlacedBattleBlock(level, pos, battleBlock));
  }

  private boolean hasPerTurnEffect(BattleBlock battleBlock) {
    return battleBlock.damagePerTurn
        || battleBlock.healingPerTurn
        || battleBlock.defencePerTurn
        || battleBlock.defenceDamagePerTurn;
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
    BattleTeam currentTeam = getCurrentTeam();
    BattleTeam enemyTeam = getEnemyTeam();

    queuePerTurnEffects(currentTeam);

    currentTeam.heal(pendingHealing);
    currentTeam.gainShield(pendingShieldGain);

    enemyTeam.takeHealthDamage(pendingDamage);
    enemyTeam.loseShield(pendingShieldDamage);

    clearPendingEffects();
    battleState.advanceTurn();
  }

  public void resetBattle() {
    clearPendingEffects();
    battleState.clearPlacedBlocks();
    battleState.resetForNewBattle();
  }

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingHealing = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }
}
