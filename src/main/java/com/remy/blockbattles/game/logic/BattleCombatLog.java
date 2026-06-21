package com.remy.blockbattles.game.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;

public final class BattleCombatLog {
  private BattleCombatLog() {
  }

  public static void broadcast(MinecraftServer server, Component message) {
    if (server == null || message == null) {
      return;
    }

    MutableComponent prefixedMessage = Component.literal("[BB] ").withStyle(ChatFormatting.GOLD)
        .append(message);

    server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(prefixedMessage));
  }

  public static void logTurnStart(
      MinecraftServer server,
      TeamSide side,
      int handSize,
      int placements,
      BattleWarp activeWarp,
      WitchHutEffect witchHutEffect) {
    MutableComponent message = teamName(side)
        .append(Component.literal(" turn started").withStyle(ChatFormatting.WHITE))
        .append(Component.literal(" | hand " + handSize).withStyle(ChatFormatting.GRAY))
        .append(Component.literal(" | placements " + placements).withStyle(ChatFormatting.GRAY));

    if (activeWarp != null && activeWarp != BattleWarp.NONE) {
      message = message.append(Component.literal(" | warp ").withStyle(ChatFormatting.DARK_GRAY))
          .append(Component.literal(activeWarp.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));

      if (activeWarp == BattleWarp.WITCH_HUT && witchHutEffect != null && witchHutEffect != WitchHutEffect.NONE) {
        message = message.append(Component.literal(" | effect ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(witchHutEffect.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
      }
    }

    broadcast(server, message);
  }

  public static void logTurnEnd(MinecraftServer server, TeamSide endedSide, TeamSide nextSide) {
    broadcast(
        server,
        teamName(endedSide)
            .append(Component.literal(" turn ended").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" | next: ").withStyle(ChatFormatting.DARK_GRAY))
            .append(teamName(nextSide)));
  }

  public static void logWarpChange(MinecraftServer server, BattleWarp previousWarp, BattleWarp nextWarp) {
    String previousName = previousWarp == null ? BattleWarp.NONE.getDisplayName() : previousWarp.getDisplayName();
    String nextName = nextWarp == null ? BattleWarp.NONE.getDisplayName() : nextWarp.getDisplayName();

    broadcast(
        server,
        Component.literal("Warp changed: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(previousName).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(nextName).withStyle(ChatFormatting.LIGHT_PURPLE)));
  }

  public static void logWitchHutEffect(MinecraftServer server, WitchHutEffect effect) {
    if (effect == null || effect == WitchHutEffect.NONE) {
      return;
    }

    broadcast(
        server,
        Component.literal("Witch Hut effect: ").withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.literal(effect.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE)));
  }

  public static void logBlockPlaced(MinecraftServer server, TeamSide side, String blockName) {
    if (blockName == null || blockName.isBlank()) {
      return;
    }

    broadcast(
        server,
        teamName(side)
            .append(Component.literal(" placed ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(blockName).withStyle(ChatFormatting.YELLOW)));
  }

  public static void logBlockActivation(
      MinecraftServer server,
      TeamSide sourceSide,
      String blockName,
      BattleFeedbackType type,
      int amount) {
    if (server == null || type == null || amount <= 0 || blockName == null || blockName.isBlank()) {
      return;
    }

    MutableComponent sourcePrefix = sourceSide == null
        ? Component.literal(blockName).withStyle(ChatFormatting.YELLOW)
        : teamName(sourceSide)
            .append(Component.literal("'s ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(blockName).withStyle(ChatFormatting.YELLOW));

    broadcast(
        server,
        sourcePrefix
            .append(Component.literal(" " + type.sourceVerb() + " ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal("+" + amount + " " + type.activationLabel()).withStyle(colorFor(type))));
  }

  public static void logTeamStatChange(MinecraftServer server, BattleTeam team, BattleFeedbackType type, int amount) {
    if (server == null || team == null || type == null || amount <= 0) {
      return;
    }

    broadcast(
        server,
        teamName(team.getSide())
            .append(Component.literal(" " + type.positiveTeamVerb() + " ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(String.valueOf(amount)).withStyle(colorFor(type))));
  }

  public static void logRevive(MinecraftServer server, TeamSide side, String sourceName, int restoredHealth) {
    if (server == null || side == null || sourceName == null || sourceName.isBlank()) {
      return;
    }

    broadcast(
        server,
        teamName(side)
            .append(Component.literal(" revived via ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(sourceName).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" at ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(String.valueOf(restoredHealth)).withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" health").withStyle(ChatFormatting.WHITE)));
  }

  public static void logEndWarpFinish(MinecraftServer server, TeamSide losingSide) {
    if (server == null || losingSide == null) {
      return;
    }

    broadcast(
        server,
        Component.literal("End Warp finished the game. ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(teamName(losingSide))
            .append(Component.literal(" was defeated.").withStyle(ChatFormatting.WHITE)));
  }

  public static String displayNameForBlock(Block block) {
    if (block == null) {
      return "Unknown Block";
    }

    String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();

    if (blockId.equals("minecraft:blast_furnace") || blockId.equals("minecraft:smoker")) {
      return "Furnace";
    }

    return com.remy.blockbattles.game.blocks.CreateBlocks.findByMinecraftId(blockId)
        .map(battleBlock -> battleBlock.displayName)
        .orElseGet(() -> block.getName().getString());
  }

  private static ChatFormatting colorFor(BattleFeedbackType type) {
    return switch (type) {
      case DAMAGE -> ChatFormatting.RED;
      case HEALING -> ChatFormatting.GREEN;
      case SHIELD_GAIN -> ChatFormatting.AQUA;
      case SHIELD_LOSS -> ChatFormatting.GOLD;
      case MAX_HEALTH -> ChatFormatting.LIGHT_PURPLE;
    };
  }

  private static MutableComponent teamName(TeamSide side) {
    ChatFormatting color = side == TeamSide.RED ? ChatFormatting.RED : ChatFormatting.BLUE;
    return Component.literal(side.getDisplayName()).withStyle(color);
  }
}
