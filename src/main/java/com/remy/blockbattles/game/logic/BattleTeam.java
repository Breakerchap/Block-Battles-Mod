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
  private int maxHealth;
  private int queuedTurnDamageBonus;
  private int queuedTurnHealingBonus;
  private int queuedTurnShieldBonus;
  private int queuedTurnDrawModifier;
  private int queuedTurnDamage;
  private int queuedTurnHealing;
  private int queuedTurnShieldGain;
  private boolean ignoreNextIncomingDamage;
  private boolean queuedHealingAlsoIncreasesMaxHealth;
  private int activeTurnDamageBonus;
  private int activeTurnHealingBonus;
  private int activeTurnShieldBonus;
  private int activeTurnDrawModifier;
  private int activeTurnDamage;
  private int activeTurnHealing;
  private int activeTurnShieldGain;
  private boolean activeHealingAlsoIncreasesMaxHealth;
  private int lastDamageDealt;
  private int lastDamageTakenFromOpponent;
  private final ArrayList<BattleBlock> hand = new ArrayList<>();
  private final ArrayList<BattleBlock> startingDeck = new ArrayList<>();
  private final ArrayList<BattleBlock> drawPile = new ArrayList<>();
  private final ArrayList<PlacedBattleBlock> placedBlocks = new ArrayList<>();

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

  public int getMaxHealth() {
    return maxHealth;
  }

  public int getMissingHealth() {
    return Math.max(0, maxHealth - health);
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

  List<PlacedBattleBlock> getPlacedBlocks() {
    return placedBlocks;
  }

  public void addPlacedBlock(PlacedBattleBlock placedBlock) {
    placedBlocks.add(Objects.requireNonNull(placedBlock, "placedBlock"));
  }

  public void clearPlacedBlocks() {
    placedBlocks.clear();
  }

  public void resetForNewBattle(int startingHealth, int startingShield, List<BattleBlock> startingDeck) {
    maxHealth = startingHealth;
    health = maxHealth;
    shield = startingShield;
    lastDamageDealt = 0;
    lastDamageTakenFromOpponent = 0;
    clearQueuedTurnEffects();
    clearActiveTurnEffects();
    hand.clear();
    clearPlacedBlocks();
    this.startingDeck.clear();
    this.startingDeck.addAll(Objects.requireNonNull(startingDeck, "startingDeck"));
    refillDrawPile();
    Collections.shuffle(drawPile);
  }

  public void refillDrawPile() {
    drawPile.clear();
    drawPile.addAll(startingDeck);
  }

  public void setDeck(List<BattleBlock> deck) {
    startingDeck.clear();
    startingDeck.addAll(Objects.requireNonNull(deck, "deck"));
    clearHand();
    refillDrawPile();
    Collections.shuffle(drawPile);
  }

  public void clearHand() {
    hand.clear();
  }

  public boolean hasCardInHand(String blockId) {
    return hand.stream().anyMatch(card -> card.id.getId().equals(blockId));
  }

  public void replaceOneCardInDeck(BattleBlock currentCard, BattleBlock replacementCard) {
    int deckIndex = startingDeck.indexOf(currentCard);

    if (deckIndex >= 0) {
      startingDeck.set(deckIndex, Objects.requireNonNull(replacementCard, "replacementCard"));
    }
  }

  public boolean removeOneCardFromDeck(BattleBlock battleBlock) {
    Objects.requireNonNull(battleBlock, "battleBlock");

    boolean removedFromDeck = startingDeck.remove(battleBlock);

    if (!removedFromDeck) {
      return false;
    }

    drawPile.remove(battleBlock);
    return true;
  }

  public void queueTurnDamageBonus(int amount) {
    queuedTurnDamageBonus += amount;
  }

  public void queueTurnHealingBonus(int amount) {
    queuedTurnHealingBonus += amount;
  }

  public void queueTurnShieldBonus(int amount) {
    queuedTurnShieldBonus += amount;
  }

  public void queueTurnDrawModifier(int amount) {
    queuedTurnDrawModifier += amount;
  }

  public void queueTurnDamage(int amount) {
    queuedTurnDamage += amount;
  }

  public void queueTurnHealing(int amount) {
    queuedTurnHealing += amount;
  }

  public void queueTurnShieldGain(int amount) {
    queuedTurnShieldGain += amount;
  }

  public void queueIgnoreNextIncomingDamage() {
    ignoreNextIncomingDamage = true;
  }

  public void queueHealingAlsoIncreasesMaxHealth() {
    queuedHealingAlsoIncreasesMaxHealth = true;
  }

  public boolean consumeIgnoreNextIncomingDamage() {
    boolean shouldIgnore = ignoreNextIncomingDamage;
    ignoreNextIncomingDamage = false;
    return shouldIgnore;
  }

  public void activateQueuedTurnEffects() {
    activeTurnDamageBonus += queuedTurnDamageBonus;
    activeTurnHealingBonus += queuedTurnHealingBonus;
    activeTurnShieldBonus += queuedTurnShieldBonus;
    activeTurnDrawModifier += queuedTurnDrawModifier;
    activeTurnDamage += queuedTurnDamage;
    activeTurnHealing += queuedTurnHealing;
    activeTurnShieldGain += queuedTurnShieldGain;
    activeHealingAlsoIncreasesMaxHealth |= queuedHealingAlsoIncreasesMaxHealth;
    clearQueuedTurnEffects();
  }

  public int getActiveTurnDamageBonus() {
    return activeTurnDamageBonus;
  }

  public int getActiveTurnHealingBonus() {
    return activeTurnHealingBonus;
  }

  public int getActiveTurnShieldBonus() {
    return activeTurnShieldBonus;
  }

  public int getActiveTurnDrawModifier() {
    return activeTurnDrawModifier;
  }

  public boolean isActiveHealingAlsoIncreasesMaxHealth() {
    return activeHealingAlsoIncreasesMaxHealth;
  }

  public int getLastDamageDealt() {
    return lastDamageDealt;
  }

  public int getLastDamageTakenFromOpponent() {
    return lastDamageTakenFromOpponent;
  }

  public void recordLastDamageDealt(int amount) {
    if (amount > 0) {
      lastDamageDealt = amount;
    }
  }

  public void recordLastDamageTakenFromOpponent(int amount) {
    lastDamageTakenFromOpponent = Math.max(0, amount);
  }

  public int consumeActiveTurnDamage() {
    int amount = activeTurnDamage;
    activeTurnDamage = 0;
    return amount;
  }

  public int consumeActiveTurnHealing() {
    int amount = activeTurnHealing;
    activeTurnHealing = 0;
    return amount;
  }

  public int consumeActiveTurnShieldGain() {
    int amount = activeTurnShieldGain;
    activeTurnShieldGain = 0;
    return amount;
  }

  public void clearActiveTurnEffects() {
    activeTurnDamageBonus = 0;
    activeTurnHealingBonus = 0;
    activeTurnShieldBonus = 0;
    activeTurnDrawModifier = 0;
    activeTurnDamage = 0;
    activeTurnHealing = 0;
    activeTurnShieldGain = 0;
    activeHealingAlsoIncreasesMaxHealth = false;
  }

  public void clearQueuedTurnEffects() {
    queuedTurnDamageBonus = 0;
    queuedTurnHealingBonus = 0;
    queuedTurnShieldBonus = 0;
    queuedTurnDrawModifier = 0;
    queuedTurnDamage = 0;
    queuedTurnHealing = 0;
    queuedTurnShieldGain = 0;
    ignoreNextIncomingDamage = false;
    queuedHealingAlsoIncreasesMaxHealth = false;
  }

  public void heal(int amount) {
    health += amount;
    health = Math.min(health, maxHealth);
  }

  public void increaseMaxHealth(int amount) {
    maxHealth = Math.max(0, maxHealth + amount);
    health = Math.min(health, maxHealth);
  }

  public int takeHealthDamage(int amount) {
    int actualDamage = Math.max(0, amount - shield);
    health -= actualDamage;
    return actualDamage;
  }

  public int takeDirectHealthDamage(int amount) {
    int actualDamage = Math.max(0, amount);
    health -= actualDamage;
    return actualDamage;
  }

  public void gainShield(int amount) {
    shield = Math.max(0, shield + amount);
  }

  public void loseShield(int amount) {
    shield = Math.max(0, shield - amount);
  }
}
