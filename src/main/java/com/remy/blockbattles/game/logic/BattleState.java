package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.CreateBlocks;

public class BattleState {
  private static final int STARTING_HEALTH = 200;
  private static final int STARTING_SHIELD = 0;
  private static final BattleState SHARED_INSTANCE = new BattleState();

  private final BattleTeam redTeam = new BattleTeam(TeamSide.RED);
  private final BattleTeam blueTeam = new BattleTeam(TeamSide.BLUE);
  private final ArrayList<BattleBlock> redConfiguredDeck = new ArrayList<>();
  private final ArrayList<BattleBlock> blueConfiguredDeck = new ArrayList<>();
  private TeamSide activeSide = TeamSide.RED;
  private boolean gameRunning;

  private BattleState() {
    redConfiguredDeck.addAll(createRedStartingDeck());
    blueConfiguredDeck.addAll(createBlueStartingDeck());
    resetForNewBattle();
  }

  BattleState(List<BattleBlock> redDeck, List<BattleBlock> blueDeck) {
    redConfiguredDeck.addAll(Objects.requireNonNull(redDeck, "redDeck"));
    blueConfiguredDeck.addAll(Objects.requireNonNull(blueDeck, "blueDeck"));
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

  public boolean isGameRunning() {
    return gameRunning;
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

  public void setGameRunning(boolean gameRunning) {
    this.gameRunning = gameRunning;
  }

  public void clearPlacedBlocks() {
    redTeam.clearPlacedBlocks();
    blueTeam.clearPlacedBlocks();
  }

  public List<BattleBlock> getConfiguredDeck(TeamSide side) {
    return Collections.unmodifiableList(getConfiguredDeckList(side));
  }

  public void setConfiguredDeck(TeamSide side, List<BattleBlock> deck) {
    ArrayList<BattleBlock> configuredDeck = getConfiguredDeckList(side);

    configuredDeck.clear();
    configuredDeck.addAll(Objects.requireNonNull(deck, "deck"));
  }

  public void applyConfiguredDeck(TeamSide side) {
    getTeam(side).setDeck(getConfiguredDeck(side));
  }

  public void resetForNewBattle() {
    clearPlacedBlocks();
    redTeam.resetForNewBattle(STARTING_HEALTH, STARTING_SHIELD, redConfiguredDeck);
    blueTeam.resetForNewBattle(STARTING_HEALTH, STARTING_SHIELD, blueConfiguredDeck);
    activeSide = TeamSide.RED;
  }

  private ArrayList<BattleBlock> getConfiguredDeckList(TeamSide side) {
    return side == TeamSide.RED ? redConfiguredDeck : blueConfiguredDeck;
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
