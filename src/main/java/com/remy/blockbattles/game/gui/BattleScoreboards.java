package com.remy.blockbattles.game.gui;

import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.BattleTeam;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
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
          Component.literal("Block Battles"),
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

    setScore(scoreboard, objective, "Red Health", redTeam.getHealth());
    setScore(scoreboard, objective, "Red Shield", redTeam.getShield());

    setScore(scoreboard, objective, "Blue Health", blueTeam.getHealth());
    setScore(scoreboard, objective, "Blue Shield", blueTeam.getShield());
  }

  private static void setScore(Scoreboard scoreboard, Objective objective, String name, int value) {
    scoreboard.getOrCreatePlayerScore(
        ScoreHolder.forNameOnly(name),
        objective).set(value);
  }
}
