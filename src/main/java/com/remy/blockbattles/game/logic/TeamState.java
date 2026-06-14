package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.List;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.CreateBlocks;

public class TeamState {
  public final TeamData redTeam;
  public final TeamData blueTeam;

  public TeamData.Team currentTurn;

  public TeamState() {
    redTeam = new TeamData(
        TeamData.Team.RED,
        100,
        0,
        new ArrayList<BattleBlock>(),
        new ArrayList<BattleBlock>());

    blueTeam = new TeamData(
        TeamData.Team.BLUE,
        100,
        0,
        new ArrayList<BattleBlock>(),
        new ArrayList<BattleBlock>());

    currentTurn = TeamData.Team.RED;

    createDecks();
  }

  private void createDecks() {
    redTeam.deck.addAll(List.of(
        CreateBlocks.SAND,
        CreateBlocks.SANDSTONE,
        CreateBlocks.SMOOTH_SANDSTONE,
        CreateBlocks.CUT_SANDSTONE,
        CreateBlocks.RED_SAND,
        CreateBlocks.RED_SANDSTONE,
        CreateBlocks.SMOOTH_RED_SANDSTONE));

    blueTeam.deck.addAll(List.of(
        CreateBlocks.CUT_RED_SANDSTONE,
        CreateBlocks.CHISELED_RED_SANDSTONE,
        CreateBlocks.HORN_CORAL_BLOCK,
        CreateBlocks.TUBE_CORAL_BLOCK,
        CreateBlocks.BUBBLE_CORAL_BLOCK,
        CreateBlocks.FIRE_CORAL_BLOCK,
        CreateBlocks.SAND));
  }
}