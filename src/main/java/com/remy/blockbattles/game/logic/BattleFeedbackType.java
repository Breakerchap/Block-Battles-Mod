package com.remy.blockbattles.game.logic;

public enum BattleFeedbackType {
  DAMAGE(0xF24040, 0.95F, "damage", "lost health", "dealt damage"),
  HEALING(0x4CE673, 1.15F, "healing", "healed health", "activated healing"),
  SHIELD_GAIN(0x40BFF2, 1.10F, "defence", "gained defence", "granted defence"),
  SHIELD_LOSS(0xFF8C33, 0.70F, "defence damage", "lost defence", "dealt defence damage"),
  MAX_HEALTH(0xFF73B3, 1.00F, "max health", "gained max health", "increased max health");

  private final int color;
  private final float pitch;
  private final String activationLabel;
  private final String positiveTeamVerb;
  private final String sourceVerb;

  BattleFeedbackType(int color, float pitch, String activationLabel, String positiveTeamVerb, String sourceVerb) {
    this.color = color;
    this.pitch = pitch;
    this.activationLabel = activationLabel;
    this.positiveTeamVerb = positiveTeamVerb;
    this.sourceVerb = sourceVerb;
  }

  public int color() {
    return color;
  }

  public float pitch() {
    return pitch;
  }

  public String activationLabel() {
    return activationLabel;
  }

  public String positiveTeamVerb() {
    return positiveTeamVerb;
  }

  public String sourceVerb() {
    return sourceVerb;
  }
}
