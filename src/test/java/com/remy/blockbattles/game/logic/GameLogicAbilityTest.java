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

  @Test
  void cornflowerHealsQuarterOfCurrentHealth() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CORNFLOWER),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(80);
    redTeam.getHand().add(CreateBlocks.CORNFLOWER);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.CORNFLOWER, TeamSide.RED));
    assertEquals(150, redTeam.getHealth());
  }

  @Test
  void lapisBlockHealsAndReplacesItselfWithDirtInDeck() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.LAPIS_BLOCK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(99);
    redTeam.getHand().add(CreateBlocks.LAPIS_BLOCK);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.LAPIS_BLOCK, TeamSide.RED));
    assertEquals(152, redTeam.getHealth());
    assertEquals(CreateBlocks.DIRT, redTeam.getStartingDeck().get(0));
  }

  @Test
  void redstoneTorchBoostsDamageOnYourNextTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.HORN_CORAL_BLOCK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    redTeam.getHand().add(CreateBlocks.REDSTONE_TORCH);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.REDSTONE_TORCH, TeamSide.RED));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.SAND, TeamSide.BLUE));
    assertTrue(redTeam.hasCardInHand(CreateBlocks.HORN_CORAL_BLOCK.id.getId()));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.HORN_CORAL_BLOCK, TeamSide.RED));
    assertEquals(177, blueTeam.getHealth());
  }

  @Test
  void witherSkeletonSkullAddsBonusDamageAgainstHighShield() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.WITHER_SKELETON_SKULL),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam blueTeam = battleState.getBlueTeam();

    blueTeam.gainShield(7);
    battleState.getRedTeam().getHand().add(CreateBlocks.WITHER_SKELETON_SKULL);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.WITHER_SKELETON_SKULL, TeamSide.RED));
    assertEquals(181, blueTeam.getHealth());
  }

  @Test
  void deadBushPerTurnDamageScalesWithOwnersMissingHealth() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.DEAD_BUSH),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = new GameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(25);
    assertEquals(7, gameLogic.getPerTurnDamageAmount(CreateBlocks.DEAD_BUSH, redTeam));
  }
}
