package com.remy.blockbattles.game.blocks;

public class BattleBlock {
  final public BattleBlockIDs id;
  final public String displayName;
  final public String abilityDescription;
  final public Classification classification;

  final public int damage;
  final public int healing;
  final public int defence;
  final public int defenceDamage;

  public BattleBlock(BattleBlockIDs id, String displayName, String abilityDescription,
      Classification classification,
      int damage, int healing, int defence, int defenceDamage) {
    this.id = id;
    this.displayName = displayName;
    this.abilityDescription = abilityDescription;
    this.classification = classification;
    this.damage = damage;
    this.healing = healing;
    this.defence = defence;
    this.defenceDamage = defenceDamage;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
