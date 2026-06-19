package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class GameLogicWarpTest {
  private static GameLogic createRunningGameLogic(BattleState battleState) {
    battleState.setGameRunning(true);
    return new GameLogic(battleState);
  }

  @Test
  void villageHouseWarpHealsBothTeamsEachTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.SAND),
        List.of(CreateBlocks.RED_SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    battleState.setActiveWarp(BattleWarp.VILLAGE_HOUSE);
    redTeam.takeHealthDamage(40);
    blueTeam.takeHealthDamage(40);

    gameLogic.endTurn(TeamSide.RED);

    assertEquals(176, redTeam.getHealth());
    assertEquals(176, blueTeam.getHealth());
  }

  @Test
  void libraryWarpDrawsEntireDeckAndRefillsWhenEmpty() {
    BattleState battleState = new BattleState(
        List.of(
            CreateBlocks.SAND,
            CreateBlocks.SANDSTONE,
            CreateBlocks.SMOOTH_SANDSTONE,
            CreateBlocks.CUT_SANDSTONE),
        List.of(CreateBlocks.RED_SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    battleState.setActiveWarp(BattleWarp.LIBRARY);

    gameLogic.forceTurn(TeamSide.RED);
    assertEquals(4, redTeam.getHandSize());
    assertEquals(0, redTeam.getDrawPileSize());

    gameLogic.forceTurn(TeamSide.RED);
    assertEquals(4, redTeam.getHandSize());
    assertEquals(0, redTeam.getDrawPileSize());
  }

  @Test
  void endWarpEndsAfterThreeRoundsAndBreaksTiesForStarter() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.SAND),
        List.of(CreateBlocks.RED_SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    battleState.setActiveWarp(BattleWarp.END);
    battleState.setActiveWarpStarterSide(TeamSide.RED);
    battleState.setActiveWarpRoundCount(2);
    redTeam.setHealth(150);
    blueTeam.setHealth(150);

    gameLogic.forceTurn(TeamSide.RED);

    assertFalse(battleState.isGameRunning());
    assertEquals(150, redTeam.getHealth());
    assertEquals(0, blueTeam.getHealth());
    assertEquals(0, battleState.getRemainingPlacementsThisTurn());
  }
}
