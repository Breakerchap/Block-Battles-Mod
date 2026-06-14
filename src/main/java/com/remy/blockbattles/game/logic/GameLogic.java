package com.remy.blockbattles.game.logic;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.logic.TeamData.Team;

public class GameLogic {
  private final TeamState teamState;

  int damageToDeal;
  int healingToHeal;
  int defenceToAdd;
  int defenceDamageToDeal;

  public GameLogic(TeamState teamState) {
    this.teamState = teamState;
  }

  void onPlaceBattleBlock(BattleBlock block) {
    damageToDeal += block.damage;
    healingToHeal += block.healing;
    defenceToAdd += block.defence;
    defenceDamageToDeal += block.defenceDamage;
  }

  private TeamData getCurrentTeam() {
    return teamState.currentTurn == Team.RED ? teamState.redTeam : teamState.blueTeam;
  }

  private TeamData getEnemyTeam() {
    return teamState.currentTurn == Team.RED ? teamState.blueTeam : teamState.redTeam;
  }

  void endTurn() {
    TeamData currentTeam = getCurrentTeam();
    TeamData enemyTeam = getEnemyTeam();

    currentTeam.health += healingToHeal;
    currentTeam.shield += defenceToAdd;

    enemyTeam.health -= damageToDeal;
    enemyTeam.shield -= defenceDamageToDeal;

    if (enemyTeam.shield < 0) {
      enemyTeam.shield = 0;
    }

    damageToDeal = 0;
    healingToHeal = 0;
    defenceToAdd = 0;
    defenceDamageToDeal = 0;

    teamState.currentTurn = teamState.currentTurn == Team.RED ? Team.BLUE : Team.RED;
  }
}