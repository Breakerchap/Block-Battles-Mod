package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class BattleTeamMaxHealthTest {
  @Test
  void resetForNewBattleInitializesMaxHealth() {
    BattleTeam team = new BattleTeam(TeamSide.RED);

    team.resetForNewBattle(100, 0, List.of(CreateBlocks.SAND));

    assertEquals(100, team.getMaxHealth());
    assertEquals(100, team.getHealth());
  }

  @Test
  void healingCapsAtMaxHealth() {
    BattleTeam team = new BattleTeam(TeamSide.RED);

    team.resetForNewBattle(100, 0, List.of(CreateBlocks.SAND));
    team.takeHealthDamage(30);
    team.heal(50);

    assertEquals(100, team.getHealth());
  }
}
