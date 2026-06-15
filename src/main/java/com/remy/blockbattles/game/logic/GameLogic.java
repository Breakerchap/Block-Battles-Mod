package com.remy.blockbattles.game.logic;

import com.remy.blockbattles.game.blocks.BattleBlock;

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

  public void onPlaceBattleBlock(BattleBlock block) {
    pendingDamage += block.damage;
    pendingHealing += block.healing;
    pendingShieldGain += block.defence;
    pendingShieldDamage += block.defenceDamage;
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

  private void clearPendingEffects() {
    pendingDamage = 0;
    pendingHealing = 0;
    pendingShieldGain = 0;
    pendingShieldDamage = 0;
  }
}
