package com.remy.blockbattles.game.logic;

import java.util.ArrayList;

import com.remy.blockbattles.game.blocks.BattleBlock;

public class TeamData {
  final Team team;

  int health;
  int shield;
  ArrayList<BattleBlock> hand = new ArrayList<>();
  ArrayList<BattleBlock> deck = new ArrayList<>();

  public TeamData(Team team, int health, int shield, ArrayList<BattleBlock> hand, ArrayList<BattleBlock> deck) {
    this.team = team;
    this.health = health;
    this.shield = shield;
    this.hand = hand;
    this.deck = deck;
  }

  public enum Team {
    RED,
    BLUE
  }
}
