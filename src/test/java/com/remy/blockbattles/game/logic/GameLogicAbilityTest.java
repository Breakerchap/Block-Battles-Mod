package com.remy.blockbattles.game.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.remy.blockbattles.game.blocks.CreateBlocks;

class GameLogicAbilityTest {
  private static GameLogic createRunningGameLogic(BattleState battleState) {
    battleState.setGameRunning(true);
    return new GameLogic(battleState);
  }

  @Test
  void cherryLeavesHealUsesTenthOfCurrentMaxHealth() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CHERRY_LEAVES),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
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
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(80);
    redTeam.getHand().add(CreateBlocks.CORNFLOWER);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.CORNFLOWER, TeamSide.RED));
    assertEquals(150, redTeam.getHealth());
  }

  @Test
  void pinkPetalsMakeNextTurnHealingIncreaseMaxHealthToo() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.PINK_PETALS, CreateBlocks.GLOWSTONE),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(50);
    redTeam.getHand().add(CreateBlocks.PINK_PETALS);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.PINK_PETALS, TeamSide.RED));
    assertEquals(160, redTeam.getHealth());
    assertEquals(200, redTeam.getMaxHealth());
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.SAND, TeamSide.BLUE));
    assertTrue(redTeam.hasCardInHand(CreateBlocks.GLOWSTONE.id.getId()));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.GLOWSTONE, TeamSide.RED));
    assertEquals(172, redTeam.getHealth());
    assertEquals(212, redTeam.getMaxHealth());
  }

  @Test
  void lapisBlockHealsAndReplacesItselfWithDirtInDeck() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.LAPIS_BLOCK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(99);
    redTeam.getHand().add(CreateBlocks.LAPIS_BLOCK);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.LAPIS_BLOCK, TeamSide.RED));
    assertEquals(152, redTeam.getHealth());
    assertEquals(CreateBlocks.DIRT, redTeam.getStartingDeck().get(0));
  }

  @Test
  void netheriteBlockReplacesItselfWithDirtInDeck() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.NETHERITE_BLOCK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.NETHERITE_BLOCK);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.NETHERITE_BLOCK, TeamSide.RED));
    assertEquals(CreateBlocks.DIRT, redTeam.getStartingDeck().get(0));
    assertEquals(8, redTeam.getShield());
  }

  @Test
  void redstoneTorchBoostsDamageOnYourNextTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.HORN_CORAL_BLOCK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
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
  void netherrackRepeatsLastDamageYouDealt() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.HORN_CORAL_BLOCK, CreateBlocks.NETHERRACK),
        List.of(CreateBlocks.MOSS_BLOCK));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    redTeam.getHand().add(CreateBlocks.HORN_CORAL_BLOCK);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.HORN_CORAL_BLOCK, TeamSide.RED));
    assertEquals(180, blueTeam.getHealth());
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.MOSS_BLOCK, TeamSide.BLUE));
    assertTrue(redTeam.hasCardInHand(CreateBlocks.NETHERRACK.id.getId()));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.NETHERRACK, TeamSide.RED));
    assertEquals(160, blueTeam.getHealth());
    assertEquals(20, blueTeam.getLastDamageTakenFromOpponent());
  }

  @Test
  void soulSandMakesOpponentDrawOneLessCardNextTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.SOUL_SAND),
        List.of(CreateBlocks.SAND, CreateBlocks.SANDSTONE, CreateBlocks.SMOOTH_SANDSTONE));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    redTeam.getHand().add(CreateBlocks.SOUL_SAND);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.SOUL_SAND, TeamSide.RED));
    assertEquals(2, blueTeam.getHandSize());
  }

  @Test
  void sculkLowersOpponentsMaxHealthImmediately() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.SCULK),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    redTeam.getHand().add(CreateBlocks.SCULK);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.SCULK, TeamSide.RED));
    assertEquals(198, blueTeam.getMaxHealth());
    assertEquals(198, blueTeam.getHealth());
  }

  @Test
  void vaultRemovesItselfFromDeckAndRewardsItsOwner() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.VAULT),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(30);
    redTeam.getHand().add(CreateBlocks.VAULT);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.VAULT, TeamSide.RED));
    assertEquals(178, redTeam.getHealth());
    assertEquals(8, redTeam.getShield());
    assertEquals(0, redTeam.getStartingDeck().size());
  }

  @Test
  void witherSkeletonSkullAddsBonusDamageAgainstHighShield() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.WITHER_SKELETON_SKULL),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam blueTeam = battleState.getBlueTeam();

    blueTeam.gainShield(7);
    battleState.getRedTeam().getHand().add(CreateBlocks.WITHER_SKELETON_SKULL);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.WITHER_SKELETON_SKULL, TeamSide.RED));
    assertEquals(181, blueTeam.getHealth());
  }

  @Test
  void lecternAddsEightDamagePerRemainingCardInHand() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.LECTERN, CreateBlocks.SAND, CreateBlocks.RED_SANDSTONE),
        List.of(CreateBlocks.MOSS_BLOCK));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam blueTeam = battleState.getBlueTeam();
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.LECTERN);
    redTeam.getHand().add(CreateBlocks.SAND);
    redTeam.getHand().add(CreateBlocks.RED_SANDSTONE);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.LECTERN, TeamSide.RED));
    assertEquals(178, blueTeam.getHealth());
  }

  @Test
  void bookshelfDrawsEntireDeckNextTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.BOOKSHELF, CreateBlocks.SAND, CreateBlocks.SANDSTONE, CreateBlocks.RED_SANDSTONE),
        List.of(CreateBlocks.MOSS_BLOCK));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.BOOKSHELF);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.BOOKSHELF, TeamSide.RED));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.MOSS_BLOCK, TeamSide.BLUE));
    assertEquals(redTeam.getStartingDeck().size(), redTeam.getHandSize());
  }

  @Test
  void deepslateGoldOreStealsHalfOfOpponentsShield() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.DEEPSLATE_GOLD_ORE),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam blueTeam = battleState.getBlueTeam();
    BattleTeam redTeam = battleState.getRedTeam();

    blueTeam.gainShield(9);
    redTeam.getHand().add(CreateBlocks.DEEPSLATE_GOLD_ORE);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.DEEPSLATE_GOLD_ORE, TeamSide.RED));
    assertEquals(4, redTeam.getShield());
    assertEquals(5, blueTeam.getShield());
  }

  @Test
  void stoneBricksSetYourShieldToSeven() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.STONE_BRICKS),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.gainShield(20);
    redTeam.getHand().add(CreateBlocks.STONE_BRICKS);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.STONE_BRICKS, TeamSide.RED));
    assertEquals(7, redTeam.getShield());
  }

  @Test
  void copperTorchBlocksAllIncomingDamageOnNextOpponentTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.COPPER_TORCH),
        List.of(CreateBlocks.HORN_CORAL_BLOCK));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.COPPER_TORCH);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.COPPER_TORCH, TeamSide.RED));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.HORN_CORAL_BLOCK, TeamSide.BLUE));
    assertEquals(200, redTeam.getHealth());
  }

  @Test
  void nightWarpPreventsHealing() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.CORNFLOWER),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    battleState.setActiveWarp(BattleWarp.NIGHT);
    redTeam.takeHealthDamage(80);
    redTeam.getHand().add(CreateBlocks.CORNFLOWER);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.CORNFLOWER, TeamSide.RED));
    assertEquals(120, redTeam.getHealth());
  }

  @Test
  void nightWarpDoublesMobHeadEffects() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.WITHER_SKELETON_SKULL),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam blueTeam = battleState.getBlueTeam();
    BattleTeam redTeam = battleState.getRedTeam();

    battleState.setActiveWarp(BattleWarp.NIGHT);
    blueTeam.gainShield(7);
    redTeam.getHand().add(CreateBlocks.WITHER_SKELETON_SKULL);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.WITHER_SKELETON_SKULL, TeamSide.RED));
    assertEquals(155, blueTeam.getHealth());
  }

  @Test
  void redBedResetsNightWarpToNone() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.RED_BED),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);

    battleState.setActiveWarp(BattleWarp.NIGHT);
    battleState.getRedTeam().getHand().add(CreateBlocks.RED_BED);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.RED_BED, TeamSide.RED));
    assertEquals(BattleWarp.NONE, battleState.getActiveWarp());
  }

  @Test
  void playerHeadGrantsAnImmediateExtraTurn() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.PLAYER_HEAD),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.PLAYER_HEAD);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.PLAYER_HEAD, TeamSide.RED));
    assertEquals(TeamSide.RED, battleState.getActiveSide());
  }

  @Test
  void vaultChoiceRemovesChosenCardAndAppliesReward() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.VAULT, CreateBlocks.SANDSTONE),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(20);

    assertTrue(gameLogic.resolveVaultChoice(TeamSide.RED, CreateBlocks.SANDSTONE));
    assertEquals(188, redTeam.getHealth());
    assertEquals(8, redTeam.getShield());
    assertEquals(List.of(CreateBlocks.VAULT), redTeam.getStartingDeck());
  }

  @Test
  void chosenBlocksCanBeQueuedForNextHand() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.SAND),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);

    gameLogic.queueChosenBlocksForNextHand(
        TeamSide.RED,
        List.of(CreateBlocks.DRAGON_EGG, CreateBlocks.CRYING_OBSIDIAN));

    assertEquals(
        List.of(CreateBlocks.DRAGON_EGG, CreateBlocks.CRYING_OBSIDIAN),
        battleState.getRedTeam().consumeQueuedNextHandCards());
  }

  @Test
  void oakPlanksAddFourMoreToYourNextHand() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.OAK_PLANKS, CreateBlocks.SAND, CreateBlocks.RED_SANDSTONE),
        List.of(CreateBlocks.MOSS_BLOCK));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.getHand().add(CreateBlocks.OAK_PLANKS);

    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.OAK_PLANKS, TeamSide.RED));
    assertTrue(gameLogic.onPlaceBattleBlock(CreateBlocks.MOSS_BLOCK, TeamSide.BLUE));
    assertEquals(
        5,
        redTeam.getHand().stream().filter(card -> card == CreateBlocks.OAK_PLANKS).count());
  }

  @Test
  void deadBushPerTurnDamageScalesWithOwnersMissingHealth() {
    BattleState battleState = new BattleState(
        List.of(CreateBlocks.DEAD_BUSH),
        List.of(CreateBlocks.SAND));
    GameLogic gameLogic = createRunningGameLogic(battleState);
    BattleTeam redTeam = battleState.getRedTeam();

    redTeam.takeHealthDamage(25);
    assertEquals(7, gameLogic.getPerTurnDamageAmount(CreateBlocks.DEAD_BUSH, redTeam));
  }
}
