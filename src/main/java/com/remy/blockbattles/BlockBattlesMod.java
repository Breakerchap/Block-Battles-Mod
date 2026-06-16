package com.remy.blockbattles;

import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.BattlePlayerTeams;
import com.remy.blockbattles.game.logic.GameLogic;
import com.remy.blockbattles.game.logic.TeamSide;
import com.remy.blockbattles.game.gui.BattleDeckBuilderMenu;
import com.remy.blockbattles.game.gui.BattleScoreboards;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockBattlesMod implements ModInitializer {
  public static final String MOD_ID = "blockbattles";
  public static final BattleState BATTLE_STATE = BattleState.shared();
  public static final GameLogic GAME_LOGIC = new GameLogic(BATTLE_STATE);

  // This logger is used to write text to the console and the log file.
  // It is considered best practice to use your mod id as the logger's name.
  // That way, it's clear which mod wrote info, warnings, and errors.
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      registerCommand(dispatcher, "BB");
      registerCommand(dispatcher, "bb");
    });

    LOGGER.info("Hello Fabric world!");
  }

  private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
    dispatcher.register(Commands.literal(commandName)
        .then(Commands.literal("join")
            .then(Commands.literal("red")
                .executes(context -> joinTeam(context.getSource(), TeamSide.RED)))
            .then(Commands.literal("blue")
                .executes(context -> joinTeam(context.getSource(), TeamSide.BLUE))))
        .then(Commands.literal("reset")
            .executes(context -> {
              BattlePlayerTeams.ensureTeams(context.getSource().getServer());
              GAME_LOGIC.resetBattle();
              GAME_LOGIC.syncBattleHands(context.getSource().getServer());
              BattleScoreboards.updateScoreboard(context.getSource().getServer(), BATTLE_STATE);
              context.getSource().sendSuccess(
                  () -> Component.literal("Block Battles has been reset to its default state."),
                  false);
              return 1;
            }))
        .then(Commands.literal("buildDeck")
            .executes(context -> openBuildDeck(context.getSource())))
        .then(Commands.literal("showScoreboards")
            .executes(context -> {
              BattleScoreboards.updateScoreboard(context.getSource().getServer(), BATTLE_STATE);
              context.getSource().sendSuccess(
                  () -> Component.literal("Block Battles scoreboard is now showing."),
                  false);
              return 1;
            })));
  }

  private static int joinTeam(CommandSourceStack source, TeamSide side)
      throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    boolean changed = BattlePlayerTeams.assignPlayerToTeam(source.getServer(), player, side);

    GAME_LOGIC.syncBattleHands(source.getServer());

    source.sendSuccess(
        () -> Component.literal(changed
            ? "You joined the " + side.getDisplayName() + " team."
            : "You are already on the " + side.getDisplayName() + " team."),
        false);

    return 1;
  }

  private static int openBuildDeck(CommandSourceStack source) throws CommandSyntaxException {
    var player = source.getPlayerOrException();
    TeamSide teamSide = BattlePlayerTeams.getTeamSide(player).orElse(null);

    if (teamSide == null) {
      player.sendSystemMessage(Component.literal("Join the Red or Blue team first with /BB join red or /BB join blue."));
      return 0;
    }

    player.openMenu(new SimpleMenuProvider(
        (containerId, inventory, menuPlayer) -> new BattleDeckBuilderMenu(containerId, inventory, GAME_LOGIC, teamSide),
        Component.literal(teamSide.getDisplayName() + " Deck Builder")));
    return 1;
  }
}
