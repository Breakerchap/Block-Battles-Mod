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
  private BattleWarp activeWarp = BattleWarp.NONE;
  private TeamSide activeWarpStarterSide;
  private int activeWarpTurnCount;
  private int activeWarpRoundCount;
  private int remainingPlacementsThisTurn = 1;

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

  public BattleWarp getActiveWarp() {
    return activeWarp;
  }

  public void setActiveWarp(BattleWarp activeWarp) {
    this.activeWarp = Objects.requireNonNull(activeWarp, "activeWarp");
  }

  public TeamSide getActiveWarpStarterSide() {
    return activeWarpStarterSide;
  }

  public void setActiveWarpStarterSide(TeamSide activeWarpStarterSide) {
    this.activeWarpStarterSide = activeWarpStarterSide;
  }

  public int getActiveWarpTurnCount() {
    return activeWarpTurnCount;
  }

  public void resetActiveWarpTurnCount() {
    activeWarpTurnCount = 0;
  }

  public void advanceActiveWarpTurnCount() {
    activeWarpTurnCount++;
  }

  public int getActiveWarpRoundCount() {
    return activeWarpRoundCount;
  }

  public void setActiveWarpRoundCount(int activeWarpRoundCount) {
    this.activeWarpRoundCount = Math.max(0, activeWarpRoundCount);
  }

  public void resetActiveWarpRoundCount() {
    activeWarpRoundCount = 0;
  }

  public void advanceActiveWarpRoundCount() {
    activeWarpRoundCount++;
  }

  public int getRemainingPlacementsThisTurn() {
    return remainingPlacementsThisTurn;
  }

  public void setRemainingPlacementsThisTurn(int remainingPlacementsThisTurn) {
    this.remainingPlacementsThisTurn = Math.max(0, remainingPlacementsThisTurn);
  }

  public int consumePlacementThisTurn() {
    if (remainingPlacementsThisTurn > 0) {
      remainingPlacementsThisTurn--;
    }

    return remainingPlacementsThisTurn;
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
    activeWarp = BattleWarp.NONE;
    activeWarpStarterSide = null;
    activeWarpTurnCount = 0;
    activeWarpRoundCount = 0;
    remainingPlacementsThisTurn = 1;
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
