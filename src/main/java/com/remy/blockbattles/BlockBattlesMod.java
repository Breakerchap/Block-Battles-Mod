package com.remy.blockbattles;

import java.util.Locale;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.gui.BattleDeckBuilderMenu;
import com.remy.blockbattles.game.gui.BattleScoreboards;
import com.remy.blockbattles.game.logic.BattlePlayerTeams;
import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.BattleTeam;
import com.remy.blockbattles.game.logic.GameLogic;
import com.remy.blockbattles.game.logic.TeamSide;
import com.remy.blockbattles.network.BattleBlockOutlinePayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockBattlesMod implements ModInitializer {
  public static final String MOD_ID = "blockbattles";
  public static final BattleState BATTLE_STATE = BattleState.shared();
  public static final GameLogic GAME_LOGIC = new GameLogic(BATTLE_STATE);

  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    PayloadTypeRegistry.clientboundPlay().register(BattleBlockOutlinePayload.TYPE, BattleBlockOutlinePayload.CODEC);

    ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
        GAME_LOGIC.syncTrackedBattleBlocks(listener.getPlayer()));

    UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
      if (hand != InteractionHand.MAIN_HAND) {
        return InteractionResult.PASS;
      }

      if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
        return InteractionResult.PASS;
      }

      if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
        return InteractionResult.PASS;
      }

      if (!GAME_LOGIC.isGameRunning()) {
        return InteractionResult.PASS;
      }

      return GAME_LOGIC.openStoredBlocksMenu(serverPlayer, serverLevel, hitResult.getBlockPos())
          ? InteractionResult.SUCCESS
          : InteractionResult.PASS;
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      registerCommand(dispatcher, "BB");
      registerCommand(dispatcher, "bb");
    });

    LOGGER.info("Block Battles initialized.");
  }

  private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
    dispatcher.register(Commands.literal(commandName)
        .then(Commands.literal("join")
            .then(Commands.literal("red")
                .executes(context -> joinTeam(context.getSource(), TeamSide.RED)))
            .then(Commands.literal("blue")
                .executes(context -> joinTeam(context.getSource(), TeamSide.BLUE))))
        .then(Commands.literal("reset")
            .executes(context -> resetBattle(context.getSource())))
        .then(Commands.literal("start")
            .executes(context -> startGame(context.getSource())))
        .then(Commands.literal("end")
            .executes(context -> endGame(context.getSource())))
        .then(Commands.literal("buildDeck")
            .executes(context -> openBuildDeck(context.getSource())))
        .then(Commands.literal("debug")
            .then(Commands.literal("skipTurn")
                .executes(context -> skipCurrentTurn(context.getSource())))
            .then(Commands.literal("setTurn")
                .then(Commands.literal("red")
                    .executes(context -> setTurn(context.getSource(), TeamSide.RED)))
                .then(Commands.literal("blue")
                    .executes(context -> setTurn(context.getSource(), TeamSide.BLUE))))
            .then(Commands.literal("redrawHand")
                .executes(context -> redrawHand(context.getSource())))
            .then(Commands.literal("showState")
                .executes(context -> showState(context.getSource())))
            .then(Commands.literal("setStat")
                .then(Commands.literal("red")
                    .then(Commands.literal("health")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamHealth(
                                context.getSource(),
                                TeamSide.RED,
                                IntegerArgumentType.getInteger(context, "value")))))
                    .then(Commands.literal("maxHealth")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamMaxHealth(
                                context.getSource(),
                                TeamSide.RED,
                                IntegerArgumentType.getInteger(context, "value")))))
                    .then(Commands.literal("shield")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamShield(
                                context.getSource(),
                                TeamSide.RED,
                                IntegerArgumentType.getInteger(context, "value"))))))
                .then(Commands.literal("blue")
                    .then(Commands.literal("health")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamHealth(
                                context.getSource(),
                                TeamSide.BLUE,
                                IntegerArgumentType.getInteger(context, "value")))))
                    .then(Commands.literal("maxHealth")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamMaxHealth(
                                context.getSource(),
                                TeamSide.BLUE,
                                IntegerArgumentType.getInteger(context, "value")))))
                    .then(Commands.literal("shield")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                            .executes(context -> setTeamShield(
                                context.getSource(),
                                TeamSide.BLUE,
                                IntegerArgumentType.getInteger(context, "value")))))))
            .then(Commands.literal("drawBlock")
                .then(Commands.literal("red")
                    .then(Commands.argument("block", StringArgumentType.greedyString())
                        .executes(context -> drawBlock(
                            context.getSource(),
                            TeamSide.RED,
                            StringArgumentType.getString(context, "block")))))
                .then(Commands.literal("blue")
                    .then(Commands.argument("block", StringArgumentType.greedyString())
                        .executes(context -> drawBlock(
                            context.getSource(),
                            TeamSide.BLUE,
                            StringArgumentType.getString(context, "block"))))))
            .then(Commands.literal("drawCards")
                .then(Commands.literal("red")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> drawCards(
                            context.getSource(),
                            TeamSide.RED,
                            IntegerArgumentType.getInteger(context, "amount")))))
                .then(Commands.literal("blue")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> drawCards(
                            context.getSource(),
                            TeamSide.BLUE,
                            IntegerArgumentType.getInteger(context, "amount"))))))
            .then(Commands.literal("clearHand")
                .then(Commands.literal("red")
                    .executes(context -> clearHand(context.getSource(), TeamSide.RED)))
                .then(Commands.literal("blue")
                    .executes(context -> clearHand(context.getSource(), TeamSide.BLUE))))
            .then(Commands.literal("refillDrawPile")
                .then(Commands.literal("red")
                    .executes(context -> refillDrawPile(context.getSource(), TeamSide.RED)))
                .then(Commands.literal("blue")
                    .executes(context -> refillDrawPile(context.getSource(), TeamSide.BLUE)))))
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

  private static int resetBattle(CommandSourceStack source) {
    BattlePlayerTeams.ensureTeams(source.getServer());
    GAME_LOGIC.resetBattle();
    refreshBattleUi(source.getServer());
    source.sendSuccess(
        () -> Component.literal("Block Battles has been reset to its default state."),
        false);
    return 1;
  }

  private static int startGame(CommandSourceStack source) {
    if (GAME_LOGIC.isGameRunning()) {
      source.sendFailure(Component.literal("Block Battles is already running."));
      return 0;
    }

    BattlePlayerTeams.ensureTeams(source.getServer());
    GAME_LOGIC.startGame();
    refreshBattleUi(source.getServer());
    source.sendSuccess(
        () -> Component.literal("Block Battles started. It is now " + BATTLE_STATE.getActiveSide().getDisplayName() + "'s turn."),
        true);
    return 1;
  }

  private static int endGame(CommandSourceStack source) {
    if (!GAME_LOGIC.isGameRunning()) {
      source.sendFailure(Component.literal("Block Battles is not running."));
      return 0;
    }

    GAME_LOGIC.endGame();
    refreshBattleUi(source.getServer());
    source.sendSuccess(
        () -> Component.literal("Block Battles ended. Normal Minecraft rules are active again until you use /BB start."),
        true);
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

  private static int skipCurrentTurn(CommandSourceStack source) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    TeamSide skippedSide = BATTLE_STATE.getActiveSide();

    GAME_LOGIC.endTurn();
    refreshBattleUi(source.getServer());

    source.sendSuccess(
        () -> Component.literal(
            skippedSide.getDisplayName()
                + " team's turn was skipped. It is now "
                + BATTLE_STATE.getActiveSide().getDisplayName()
                + "'s turn."),
        true);
    return 1;
  }

  private static int setTurn(CommandSourceStack source, TeamSide side) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.forceTurn(side);
    refreshBattleUi(source.getServer());

    source.sendSuccess(
        () -> Component.literal("It is now " + side.getDisplayName() + "'s turn."),
        true);
    return 1;
  }

  private static int redrawHand(CommandSourceStack source) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    TeamSide activeSide = BATTLE_STATE.getActiveSide();

    GAME_LOGIC.redrawActiveHand();
    refreshBattleUi(source.getServer());

    source.sendSuccess(
        () -> Component.literal(activeSide.getDisplayName() + " hand redrawn."),
        true);
    return 1;
  }

  private static int setTeamHealth(CommandSourceStack source, TeamSide side, int value) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.setTeamHealth(side, value);
    BattleScoreboards.updateScoreboard(source.getServer(), BATTLE_STATE);
    source.sendSuccess(
        () -> Component.literal(side.getDisplayName() + " health set to " + BATTLE_STATE.getTeam(side).getHealth() + "."),
        true);
    return 1;
  }

  private static int setTeamMaxHealth(CommandSourceStack source, TeamSide side, int value) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.setTeamMaxHealth(side, value);
    BattleScoreboards.updateScoreboard(source.getServer(), BATTLE_STATE);
    source.sendSuccess(
        () -> Component.literal(side.getDisplayName() + " max health set to " + BATTLE_STATE.getTeam(side).getMaxHealth() + "."),
        true);
    return 1;
  }

  private static int setTeamShield(CommandSourceStack source, TeamSide side, int value) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.setTeamShield(side, value);
    BattleScoreboards.updateScoreboard(source.getServer(), BATTLE_STATE);
    source.sendSuccess(
        () -> Component.literal(side.getDisplayName() + " shield set to " + BATTLE_STATE.getTeam(side).getShield() + "."),
        true);
    return 1;
  }

  private static int drawBlock(CommandSourceStack source, TeamSide side, String blockQuery) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    BattleBlock battleBlock = resolveBattleBlock(blockQuery);

    if (battleBlock == null) {
      source.sendFailure(Component.literal("Unknown battle block '" + blockQuery + "'. Try dirt, minecraft:dirt, or Red Tulip."));
      return 0;
    }

    GAME_LOGIC.addCardToHand(side, battleBlock);
    GAME_LOGIC.syncBattleHands(source.getServer());
    source.sendSuccess(
        () -> Component.literal(battleBlock.displayName + " added to " + side.getDisplayName() + " hand."),
        true);
    return 1;
  }

  private static int drawCards(CommandSourceStack source, TeamSide side, int amount) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.drawCardsForTeam(side, amount);
    GAME_LOGIC.syncBattleHands(source.getServer());
    source.sendSuccess(
        () -> Component.literal("Drew " + amount + " card(s) for " + side.getDisplayName() + "."),
        true);
    return 1;
  }

  private static int clearHand(CommandSourceStack source, TeamSide side) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.clearHand(side);
    GAME_LOGIC.syncBattleHands(source.getServer());
    source.sendSuccess(
        () -> Component.literal(side.getDisplayName() + " hand cleared."),
        true);
    return 1;
  }

  private static int refillDrawPile(CommandSourceStack source, TeamSide side) {
    if (!requireRunningBattle(source)) {
      return 0;
    }

    GAME_LOGIC.refillDrawPile(side);
    source.sendSuccess(
        () -> Component.literal(side.getDisplayName() + " draw pile refilled and shuffled."),
        true);
    return 1;
  }

  private static int showState(CommandSourceStack source) {
    BattleTeam redTeam = BATTLE_STATE.getRedTeam();
    BattleTeam blueTeam = BATTLE_STATE.getBlueTeam();

    source.sendSuccess(
        () -> Component.literal("Status: " + (GAME_LOGIC.isGameRunning() ? "Running" : "Stopped")),
        false);
    source.sendSuccess(
        () -> Component.literal("Active Turn: " + BATTLE_STATE.getActiveSide().getDisplayName()),
        false);
    source.sendSuccess(
        () -> Component.literal("Warp: " + BATTLE_STATE.getActiveWarp().getDisplayName()
            + " | Placements Left: " + BATTLE_STATE.getRemainingPlacementsThisTurn()),
        false);
    source.sendSuccess(
        () -> Component.literal("Red - HP " + redTeam.getHealth() + "/" + redTeam.getMaxHealth()
            + ", Shield " + redTeam.getShield()
            + ", Hand [" + formatHand(redTeam) + "]"
            + ", Draw Pile " + redTeam.getDrawPileSize()),
        false);
    source.sendSuccess(
        () -> Component.literal("Blue - HP " + blueTeam.getHealth() + "/" + blueTeam.getMaxHealth()
            + ", Shield " + blueTeam.getShield()
            + ", Hand [" + formatHand(blueTeam) + "]"
            + ", Draw Pile " + blueTeam.getDrawPileSize()),
        false);
    return 1;
  }

  private static void refreshBattleUi(MinecraftServer server) {
    GAME_LOGIC.syncBattleHands(server);
    GAME_LOGIC.syncTrackedBattleBlocks(server);
    BattleScoreboards.updateScoreboard(server, BATTLE_STATE);
  }

  private static boolean requireRunningBattle(CommandSourceStack source) {
    if (GAME_LOGIC.isGameRunning()) {
      return true;
    }

    source.sendFailure(Component.literal("Block Battles is not running. Use /BB start first."));
    return false;
  }

  private static BattleBlock resolveBattleBlock(String blockQuery) {
    String trimmed = blockQuery.trim();

    if (trimmed.isEmpty()) {
      return null;
    }

    String lowered = trimmed.toLowerCase(Locale.ROOT);
    BattleBlock directMatch = CreateBlocks.findByMinecraftId(lowered).orElse(null);

    if (directMatch != null) {
      return directMatch;
    }

    String normalizedId = lowered.replace(' ', '_');

    if (!normalizedId.contains(":")) {
      BattleBlock namespacedMatch = CreateBlocks.findByMinecraftId("minecraft:" + normalizedId).orElse(null);

      if (namespacedMatch != null) {
        return namespacedMatch;
      }
    }

    String displayLike = trimmed.replace('_', ' ').trim();
    String enumLike = normalizedId.contains(":")
        ? normalizedId.substring(normalizedId.indexOf(':') + 1)
        : normalizedId;

    for (BattleBlock battleBlock : CreateBlocks.ALL) {
      if (battleBlock.displayName.equalsIgnoreCase(displayLike)
          || battleBlock.id.name().equalsIgnoreCase(enumLike)
          || battleBlock.id.getId().equalsIgnoreCase(lowered)) {
        return battleBlock;
      }
    }

    return null;
  }

  private static String formatHand(BattleTeam team) {
    if (team.getHand().isEmpty()) {
      return "empty";
    }

    return team.getHand().stream()
        .map(BlockBattlesMod::formatBattleBlock)
        .collect(Collectors.joining(", "));
  }

  private static String formatBattleBlock(BattleBlock battleBlock) {
    return battleBlock.displayName;
  }
}
