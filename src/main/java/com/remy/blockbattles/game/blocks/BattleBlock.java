package com.remy.blockbattles.game.blocks;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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

  public String requirementDescription = "";
  public Set<String> requiredSupportBlockIds = Set.of();

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

  public BattleBlock withPlacementRequirements(
      String requirementDescription,
      BattleBlockIDs... requiredSupportBlocks) {
    this.requirementDescription = requirementDescription;
    this.requiredSupportBlockIds = Arrays.stream(requiredSupportBlocks)
        .map(BattleBlockIDs::getId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    return this;
  }

  public boolean hasPlacementRequirements() {
    return !requiredSupportBlockIds.isEmpty();
  }

  public boolean canBePlacedOn(String supportBlockId) {
    return !hasPlacementRequirements() || requiredSupportBlockIds.contains(supportBlockId);
  }
}
