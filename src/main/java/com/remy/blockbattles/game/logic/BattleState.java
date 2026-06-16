package com.remy.blockbattles.game.logic;

import java.util.List;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.CreateBlocks;

public class BattleState {
  private static final int STARTING_HEALTH = 200;
  private static final int STARTING_SHIELD = 0;
  private static final BattleState SHARED_INSTANCE = new BattleState();

  private final BattleTeam redTeam = new BattleTeam(TeamSide.RED);
  private final BattleTeam blueTeam = new BattleTeam(TeamSide.BLUE);
  private TeamSide activeSide = TeamSide.RED;

  private BattleState() {
    resetForNewBattle();
  }

  public static BattleState shared() {
    return SHARED_INSTANCE;
  }

  public BattleTeam getTeam(TeamSide side) {
    return side == TeamSide.RED ? redTeam : blueTeam;
  }

  public BattleTeam getRedTeam() {
    return redTeam;
  }

  public BattleTeam getBlueTeam() {
    return blueTeam;
  }

  public TeamSide getActiveSide() {
    return activeSide;
  }

  public BattleTeam getActiveTeam() {
    return getTeam(activeSide);
  }

  public BattleTeam getWaitingTeam() {
    return getOpponentOf(activeSide);
  }

  public BattleTeam getOpponentOf(TeamSide side) {
    return getTeam(side.otherSide());
  }

  public void advanceTurn() {
    activeSide = activeSide.otherSide();
  }

  public void setActiveSide(TeamSide side) {
    activeSide = side;
  }

  public void clearPlacedBlocks() {
    redTeam.clearPlacedBlocks();
    blueTeam.clearPlacedBlocks();
  }

  public void resetForNewBattle() {
    clearPlacedBlocks();
    redTeam.resetForNewBattle(STARTING_HEALTH, STARTING_SHIELD, createRedStartingDeck());
    blueTeam.resetForNewBattle(STARTING_HEALTH, STARTING_SHIELD, createBlueStartingDeck());
    activeSide = TeamSide.RED;
  }

  private List<BattleBlock> createRedStartingDeck() {
    return List.of(
        CreateBlocks.SAND,
        CreateBlocks.SANDSTONE,
        CreateBlocks.SMOOTH_SANDSTONE,
        CreateBlocks.CUT_SANDSTONE,
        CreateBlocks.RED_SAND,
        CreateBlocks.RED_SANDSTONE,
        CreateBlocks.SMOOTH_RED_SANDSTONE);
  }

  private List<BattleBlock> createBlueStartingDeck() {
    return List.of(
        CreateBlocks.CUT_RED_SANDSTONE,
        CreateBlocks.CHISELED_RED_SANDSTONE,
        CreateBlocks.HORN_CORAL_BLOCK,
        CreateBlocks.TUBE_CORAL_BLOCK,
        CreateBlocks.BUBBLE_CORAL_BLOCK,
        CreateBlocks.FIRE_CORAL_BLOCK,
        CreateBlocks.SAND);
  }
}
