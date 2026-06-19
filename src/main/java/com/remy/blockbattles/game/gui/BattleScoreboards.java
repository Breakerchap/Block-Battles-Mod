package com.remy.blockbattles.game.gui;

import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.BattleTeam;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class BattleScoreboards {
  private static final String SIDEBAR_NAME = "bb_sidebar";

  public static void createScoreboard(MinecraftServer server) {
    Scoreboard scoreboard = server.getScoreboard();

    Objective objective = scoreboard.getObjective(SIDEBAR_NAME);

    if (objective == null) {
      objective = scoreboard.addObjective(
          SIDEBAR_NAME,
          ObjectiveCriteria.DUMMY,
          Component.literal("Block Battles").withStyle(ChatFormatting.GOLD),
          ObjectiveCriteria.RenderType.INTEGER,
          false,
          null);
    }

    scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
  }

  public static void updateScoreboard(MinecraftServer server) {
    updateScoreboard(server, BattleState.shared());
  }

  public static void updateScoreboard(MinecraftServer server, BattleState battleState) {
    Scoreboard scoreboard = server.getScoreboard();

    Objective objective = scoreboard.getObjective(SIDEBAR_NAME);

    if (objective == null) {
      createScoreboard(server);
      objective = scoreboard.getObjective(SIDEBAR_NAME);
    }

    BattleTeam redTeam = battleState.getRedTeam();
    BattleTeam blueTeam = battleState.getBlueTeam();

    BattleTeam currentTurn = battleState.getActiveTeam();

    setScore(
        scoreboard,
        objective,
        "bb_red_health",
        Component.literal("Red Health").withStyle(ChatFormatting.RED),
        redTeam.getHealth(),
        7);

    setScore(
        scoreboard,
        objective,
        "bb_red_shield",
        Component.literal("Red Shield").withStyle(ChatFormatting.RED),
        redTeam.getShield(),
        6);

    setScore(
        scoreboard,
        objective,
        "bb_blue_health",
        Component.literal("Blue Health").withStyle(ChatFormatting.BLUE),
        blueTeam.getHealth(),
        5);

    setScore(
        scoreboard,
        objective,
        "bb_blue_shield",
        Component.literal("Blue Shield").withStyle(ChatFormatting.BLUE),
        blueTeam.getShield(),
        4);

    setTextLine(
        scoreboard,
        objective,
        "bb_warp",
        Component.literal("Warp: ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(battleState.getActiveWarp().getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE)),
        3);

    setTextLine(
        scoreboard,
        objective,
        "bb_status",
        Component.literal("Status: ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(battleState.isGameRunning() ? "Running" : "Stopped")
                .withStyle(battleState.isGameRunning() ? ChatFormatting.GREEN : ChatFormatting.GRAY)),
        2);

    setTextLine(
        scoreboard,
        objective,
        "bb_turn",
        Component.literal("Turn: ")
            .withStyle(ChatFormatting.WHITE)
            .append(Component.literal(currentTurn.getName())
                .withStyle(currentTurn == redTeam ? ChatFormatting.RED : ChatFormatting.BLUE)),
        1);
  }

  private static void setScore(
      Scoreboard scoreboard,
      Objective objective,
      String key,
      Component label,
      int value,
      int order) {

    ScoreAccess score = scoreboard.getOrCreatePlayerScore(
        ScoreHolder.forNameOnly(key),
        objective);

    score.set(order);
    score.display(label);
    score.numberFormatOverride(
        new FixedFormat(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN)));
  }

  private static void setTextLine(
      Scoreboard scoreboard,
      Objective objective,
      String key,
      Component label,
      int order) {

    ScoreAccess score = scoreboard.getOrCreatePlayerScore(
        ScoreHolder.forNameOnly(key),
        objective);

    score.set(order);
    score.display(label);
    score.numberFormatOverride(new FixedFormat(Component.empty()));
  }
}
