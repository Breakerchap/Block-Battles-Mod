package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class GameLogicPlacementRequirementTest {
  private static GameLogic createRunningGameLogic(BattleState battleState) {
    battleState.setGameRunning(true);
    return new GameLogic(battleState);
  }

  @Test
  void grassVariantRequirementAcceptsMatchingNonBattleSupport() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.PINK_PETALS),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    battleState.getRedTeam().getHand().add(CreateBlocks.PINK_PETALS);

    assertTrue(gameLogic.canPlaceBattleBlock(
        CreateBlocks.PINK_PETALS.id.getId(),
        TeamSide.RED,
        CreateBlocks.GRASS_BLOCK.id.getId(),
        null));
    assertNull(gameLogic.getBattleBlockPlacementFailure(
        CreateBlocks.PINK_PETALS.id.getId(),
        TeamSide.RED,
        CreateBlocks.GRASS_BLOCK.id.getId(),
        null));
  }

  @Test
  void requirementRejectsWrongSupportBlock() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.PINK_PETALS),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    battleState.getRedTeam().getHand().add(CreateBlocks.PINK_PETALS);

    assertFalse(gameLogic.canPlaceBattleBlock(
        CreateBlocks.PINK_PETALS.id.getId(),
        TeamSide.RED,
        CreateBlocks.SAND.id.getId(),
        null));
    assertEquals(
        "Requirements: " + CreateBlocks.PINK_PETALS.requirementDescription,
        gameLogic.getBattleBlockPlacementFailure(
            CreateBlocks.PINK_PETALS.id.getId(),
            TeamSide.RED,
            CreateBlocks.SAND.id.getId(),
            null));
  }

  @Test
  void requirementRejectsEnemyOwnedBattleSupport() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CANDLE),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    battleState.getRedTeam().getHand().add(CreateBlocks.CANDLE);

    assertFalse(gameLogic.canPlaceBattleBlock(
        CreateBlocks.CANDLE.id.getId(),
        TeamSide.RED,
        CreateBlocks.CAKE.id.getId(),
        TeamSide.BLUE));
    assertEquals(
        "You can only place that on your own team's battle blocks.",
        gameLogic.getBattleBlockPlacementFailure(
            CreateBlocks.CANDLE.id.getId(),
            TeamSide.RED,
            CreateBlocks.CAKE.id.getId(),
            TeamSide.BLUE));
  }

  @Test
  void requirementAllowsFriendlyOwnedBattleSupport() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CANDLE),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    battleState.getRedTeam().getHand().add(CreateBlocks.CANDLE);

    assertTrue(gameLogic.canPlaceBattleBlock(
        CreateBlocks.CANDLE.id.getId(),
        TeamSide.RED,
        CreateBlocks.CAKE.id.getId(),
        TeamSide.RED));
  }

  @Test
  void blockDataExposesRequirementMappings() {
    assertTrue(CreateBlocks.CHERRY_LEAVES.hasPlacementRequirements());
    assertTrue(CreateBlocks.CHERRY_LEAVES.canBePlacedOn(CreateBlocks.CHERRY_LOG.id.getId()));
    assertTrue(CreateBlocks.CHERRY_LEAVES.canBePlacedOn(CreateBlocks.PALE_OAK_LOG.id.getId()));
    assertFalse(CreateBlocks.CHERRY_LEAVES.canBePlacedOn(CreateBlocks.SAND.id.getId()));
  }
}
