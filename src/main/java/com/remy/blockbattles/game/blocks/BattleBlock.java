package com.remy.blockbattles.game.blocks;

public class BattleBlock {
  final public BattleBlockIDs id;
  final public String displayName;
  final public String abilityDescription;
  final public Classification classification;

  final public int damage;
  public boolean damagePerTurn = false;

  final public int healing;
  public boolean healingPerTurn = false;

  final public int defence;
  public boolean defencePerTurn = false;

  final public int defenceDamage;
  public boolean defenceDamagePerTurn = false;

  public BattleBlock(
      BattleBlockIDs id,
      String displayName,
      String abilityDescription,
      Classification classification,
      int damage,
      int healing,
      int defence,
      int defenceDamage) {
    this(
        id,
        displayName,
        abilityDescription,
        classification,
        damage,
        false,
        healing,
        false,
        defence,
        false,
        defenceDamage,
        false);
  }

  public BattleBlock(
      BattleBlockIDs id,
      String displayName,
      String abilityDescription,
      Classification classification,
      int damage,
      boolean damagePerTurn,
      int healing,
      boolean healingPerTurn,
      int defence,
      boolean defencePerTurn,
      int defenceDamage,
      boolean defenceDamagePerTurn) {
    this.id = id;
    this.displayName = displayName;
    this.abilityDescription = abilityDescription;
    this.classification = classification;

    this.damage = damage;
    this.damagePerTurn = damagePerTurn;

    this.healing = healing;
    this.healingPerTurn = healingPerTurn;

    this.defence = defence;
    this.defencePerTurn = defencePerTurn;

    this.defenceDamage = defenceDamage;
    this.defenceDamagePerTurn = defenceDamagePerTurn;
  }

  @Override
  public String toString() {
    return displayName;
  }
}