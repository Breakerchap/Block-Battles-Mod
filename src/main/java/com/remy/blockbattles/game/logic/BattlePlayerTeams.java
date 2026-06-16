package com.remy.blockbattles.game.logic;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class BattlePlayerTeams {
  private static final String RED_TEAM_NAME = "Red";
  private static final String BLUE_TEAM_NAME = "Blue";

  private BattlePlayerTeams() {
  }

  public static void ensureTeams(MinecraftServer server) {
    Scoreboard scoreboard = server.getScoreboard();

    ensureTeam(scoreboard, TeamSide.RED);
    ensureTeam(scoreboard, TeamSide.BLUE);
  }

  public static boolean assignPlayerToTeam(MinecraftServer server, Player player, TeamSide side) {
    Scoreboard scoreboard = server.getScoreboard();
    String scoreboardName = player.getScoreboardName();
    PlayerTeam desiredTeam = ensureTeam(scoreboard, side);
    PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreboardName);

    if (currentTeam == desiredTeam) {
      return false;
    }

    if (currentTeam != null) {
      scoreboard.removePlayerFromTeam(scoreboardName);
    }

    scoreboard.addPlayerToTeam(scoreboardName, desiredTeam);
    return true;
  }

  public static Optional<TeamSide> getTeamSide(Player player) {
    if (player == null) {
      return Optional.empty();
    }

    PlayerTeam team = player.getTeam();

    if (team == null) {
      return Optional.empty();
    }

    return fromTeamName(team.getName());
  }

  private static PlayerTeam ensureTeam(Scoreboard scoreboard, TeamSide side) {
    String teamName = getTeamName(side);
    PlayerTeam team = scoreboard.getPlayerTeam(teamName);

    if (team == null) {
      team = scoreboard.addPlayerTeam(teamName);
    }

    team.setDisplayName(Component.literal(teamName).withStyle(getColor(side)));
    team.setColor(getColor(side));
    return team;
  }

  private static Optional<TeamSide> fromTeamName(String teamName) {
    if (RED_TEAM_NAME.equalsIgnoreCase(teamName)) {
      return Optional.of(TeamSide.RED);
    }

    if (BLUE_TEAM_NAME.equalsIgnoreCase(teamName)) {
      return Optional.of(TeamSide.BLUE);
    }

    return Optional.empty();
  }

  private static String getTeamName(TeamSide side) {
    return side == TeamSide.RED ? RED_TEAM_NAME : BLUE_TEAM_NAME;
  }

  private static ChatFormatting getColor(TeamSide side) {
    return side == TeamSide.RED ? ChatFormatting.RED : ChatFormatting.BLUE;
  }
}
