package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class BattleTeamTest {
  @Test
  void defenceReducesEachIncomingHitWithoutBeingConsumed() {
    BattleTeam team = new BattleTeam(TeamSide.RED);

    team.resetForNewBattle(10, 3, List.of(CreateBlocks.SAND));
    team.takeHealthDamage(5);
    team.takeHealthDamage(3);

    assertEquals(8, team.getHealth());
    assertEquals(3, team.getShield());
  }

  @Test
  void negativeShieldGainCannotDropDefenceBelowZero() {
    BattleTeam team = new BattleTeam(TeamSide.RED);

    team.resetForNewBattle(10, 2, List.of(CreateBlocks.SAND));
    team.gainShield(-5);

    assertEquals(0, team.getShield());
  }
}
