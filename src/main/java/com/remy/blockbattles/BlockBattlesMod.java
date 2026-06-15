package com.remy.blockbattles;

import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.GameLogic;
import com.remy.blockbattles.game.gui.BattleScoreboards;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
        .then(Commands.literal("showScoreboards")
            .executes(context -> {
              BattleScoreboards.updateScoreboard(context.getSource().getServer(), BATTLE_STATE);
              context.getSource().sendSuccess(
                  () -> Component.literal("Block Battles scoreboard is now showing."),
                  false);
              return 1;
            })));
  }
}
