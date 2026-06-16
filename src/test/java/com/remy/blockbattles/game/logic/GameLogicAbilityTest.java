package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class GameLogicAbilityTest {
  @Test
  void cherryLeavesHealUsesTenthOfCurrentMaxHealth() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CHERRY_LEAVES),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.increaseMaxHealth(30);
    redTeam.takeHealthDamage(50);
    redTeam.getHand().add(CreateBlocks.CHERRY_LEAVES);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.CHERRY_LEAVES, TeamSide.RED));
    assertEquals(230, redTeam.getMaxHealth());
    assertEquals(173, redTeam.getHealth());
    assertEquals(TeamSide.BLUE, battleState.getActiveSide());
  }
}
