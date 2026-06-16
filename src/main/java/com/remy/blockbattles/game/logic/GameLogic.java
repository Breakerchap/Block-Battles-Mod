package com.remy.blockbattles.game.logic;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.BattleBlockIDs;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.blocks.abilities.Abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

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

    boolean wasBattleBlock = onPlaceBattleBlock(blockId);

    return wasTnt || wasBattleBlock;
  }

  public boolean onPlaceBattleBlock(BattleBlock battleBlock) {
    queueBlockEffects(battleBlock);

    endTurn();
    return true;
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

  private void queueBlockEffects(BattleBlock battleBlock) {
    pendingDamage += battleBlock.damage;
    pendingHealing += battleBlock.healing;
    pendingShieldGain += battleBlock.defence;
    pendingShieldDamage += battleBlock.defenceDamage;
  }

  public void endTurn() {
    BattleTeam currentTeam = getCurrentTeam();
    BattleTeam enemyTeam = getEnemyTeam();

    currentTeam.heal(pendingHealing);
    currentTeam.gainShield(pendingShieldGain);

    enemyTeam.takeHealthDamage(pendingDamage);
    enemyTeam.loseShield(pendingShieldDamage);

    clearPendingEffects();
    battleState.advanceTurn();
  }

  public void resetBattle() {
    clearPendingEffects();
    battleState.resetForNewBattle();
  }

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingHealing = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }
}