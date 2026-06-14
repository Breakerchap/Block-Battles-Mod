package com.remy.blockbattles.game.blocks;

public class BattleBlock {
  final BattleBlockIDs id;
  final String displayName;
  final String abilityDescription;
  final Classification classification;

  final int damage;
  final int healing;
  final int defence;
  final int defenceDamage;

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
