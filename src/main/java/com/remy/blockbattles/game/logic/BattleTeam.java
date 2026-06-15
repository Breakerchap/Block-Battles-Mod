package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;

public class BattleTeam {
  private final TeamSide side;
  private final String name;

  private int health;
  private int shield;
  private final ArrayList<BattleBlock> hand = new ArrayList<>();
  private final ArrayList<BattleBlock> startingDeck = new ArrayList<>();
  private final ArrayList<BattleBlock> drawPile = new ArrayList<>();

  public BattleTeam(TeamSide side) {
    this.side = Objects.requireNonNull(side, "side");
    this.name = side.getDisplayName();
  }

  public TeamSide getSide() {
    return side;
  }

  public String getName() {
    return name;
  }

  public int getHealth() {
    return health;
  }

  public int getShield() {
    return shield;
  }

  public List<BattleBlock> getStartingDeck() {
    return Collections.unmodifiableList(startingDeck);
  }

  public List<BattleBlock> getDrawPile() {
    return drawPile;
  }

  public int getDrawPileSize() {
    return drawPile.size();
  }

  public List<BattleBlock> getHand() {
    return hand;
  }

  public int getHandSize() {
    return hand.size();
  }

  public void resetForNewBattle(int startingHealth, int startingShield, List<BattleBlock> startingDeck) {
    health = startingHealth;
    shield = startingShield;
    hand.clear();
    this.startingDeck.clear();
    this.startingDeck.addAll(Objects.requireNonNull(startingDeck, "startingDeck"));
    refillDrawPile();
  }

  public void refillDrawPile() {
    drawPile.clear();
    drawPile.addAll(startingDeck);
  }

  public void heal(int amount) {
    health += amount;
  }

  public void takeHealthDamage(int amount) {
    health -= Math.max(0, amount - shield);
  }

  public void gainShield(int amount) {
    shield = Math.max(0, shield + amount);
  }

  public void loseShield(int amount) {
    shield = Math.max(0, shield - amount);
  }
}
