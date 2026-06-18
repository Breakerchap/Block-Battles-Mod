package com.remy.blockbattles.game.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.logic.BattlePlayerTeams;
import com.remy.blockbattles.game.logic.BattleState;
import com.remy.blockbattles.game.logic.BattleTeam;
import com.remy.blockbattles.game.logic.TeamSide;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

public final class BattleCardItems {
  public static final int DECK_SIZE = 12;
  public static final int HAND_SIZE = 3;

  private static final String CARD_NAME_PREFIX = "[BB Card] ";

  private BattleCardItems() {
  }

  public static ItemStack createDeckDisplayItem(BattleBlock battleBlock) {
    return createBlockItem(battleBlock, false);
  }

  public static ItemStack createHandItem(BattleBlock battleBlock) {
    return createBlockItem(battleBlock, true);
  }

  public static ItemStack createNamedItem(Item item, Component name, List<Component> lore) {
    ItemStack stack = new ItemStack(item);

    stack.set(DataComponents.CUSTOM_NAME, name);
    stack.set(DataComponents.LORE, new ItemLore(lore));
    return stack;
  }

  public static boolean isSelectableForDeck(BattleBlock battleBlock) {
    return isChoosable(battleBlock) && hasPlayablePlacementItem(battleBlock);
  }

  public static String getDeckRestrictionMessage(BattleBlock battleBlock) {
    if (!isChoosable(battleBlock)) {
      return battleBlock.displayName + " is marked unchoosable.";
    }

    return battleBlock.displayName + " cannot be added yet because it is not placed as a normal block item.";
  }

  public static boolean isBattleCard(ItemStack stack) {
    Component customName = stack.getCustomName();

    return customName != null
        && customName.getString().startsWith(CARD_NAME_PREFIX);
  }

  public static void syncHands(MinecraftServer server, BattleState battleState) {
    BattleTeam activeTeam = battleState.getActiveTeam();
    TeamSide activeSide = battleState.getActiveSide();

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      clearBattleCards(player);

      if (!battleState.isGameRunning()) {
        continue;
      }

      if (BattlePlayerTeams.getTeamSide(player).orElse(null) != activeSide) {
        continue;
      }

      for (BattleBlock battleBlock : activeTeam.getHand()) {
        player.getInventory().placeItemBackInInventory(createHandItem(battleBlock));
      }
    }
  }

  private static ItemStack createBlockItem(BattleBlock battleBlock, boolean asHandCard) {
    ArrayList<Component> lore = new ArrayList<>();

    if (asHandCard) {
      lore.add(Component.literal("Block Battles Card").withStyle(ChatFormatting.GOLD));
    }

    lore.add(Component.literal("Classification: ")
        .withStyle(ChatFormatting.GRAY)
        .append(Component.literal(formatClassification(battleBlock.classification)).withStyle(ChatFormatting.LIGHT_PURPLE)));
    lore.add(Component.literal("Ability: " + battleBlock.abilityDescription).withStyle(ChatFormatting.GRAY));
    lore.add(statLine("Damage", battleBlock.damage, battleBlock.damagePerTurn, ChatFormatting.RED));
    lore.add(statLine("Defence", battleBlock.defence, battleBlock.defencePerTurn, ChatFormatting.AQUA));
    lore.add(statLine("Healing", battleBlock.healing, battleBlock.healingPerTurn, ChatFormatting.GREEN));
    lore.add(statLine(
        "Defence Damage",
        battleBlock.defenceDamage,
        battleBlock.defenceDamagePerTurn,
        ChatFormatting.GOLD));

    if (!isSelectableForDeck(battleBlock)) {
      lore.add(Component.literal(getDeckRestrictionMessage(battleBlock)).withStyle(ChatFormatting.DARK_RED));
    }

    return createNamedItem(
        resolveDisplayItem(battleBlock),
        Component.literal((asHandCard ? CARD_NAME_PREFIX : "") + battleBlock.displayName)
            .withStyle(asHandCard ? ChatFormatting.YELLOW : ChatFormatting.WHITE),
        lore);
  }

  private static Component statLine(String label, int value, boolean perTurn, ChatFormatting statColor) {
    String suffix = perTurn ? " / turn" : "";

    return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
        .append(Component.literal(formatSignedValue(value) + suffix).withStyle(statColor));
  }

  private static String formatSignedValue(int value) {
    if (value > 0) {
      return "+" + value;
    }

    return String.valueOf(value);
  }

  private static String formatClassification(com.remy.blockbattles.game.blocks.Classification classification) {
    return switch (classification) {
      case NONE -> "None";
      case NATURAL -> "Natural";
      case MAN_MADE -> "Man-made";
      case CAVE -> "Cave";
      case OTHERWORLDLY -> "Otherworldly";
    };
  }

  private static Item resolveDisplayItem(BattleBlock battleBlock) {
    Identifier identifier = Identifier.parse(battleBlock.id.getId());
    Item item = BuiltInRegistries.ITEM.get(identifier)
        .map(reference -> reference.value())
        .orElse(Items.AIR);

    if (item != Items.AIR) {
      return item;
    }

    String blockId = battleBlock.id.getId();

    if ("minecraft:water".equals(blockId)) {
      return Items.WATER_BUCKET;
    }

    if ("minecraft:lava".equals(blockId)) {
      return Items.LAVA_BUCKET;
    }

    return Items.BARRIER;
  }

  private static boolean isChoosable(BattleBlock battleBlock) {
    return !battleBlock.abilityDescription.toLowerCase(Locale.ROOT).contains("unchoosable");
  }

  private static boolean hasPlayablePlacementItem(BattleBlock battleBlock) {
    Identifier identifier = Identifier.parse(battleBlock.id.getId());
    Item item = BuiltInRegistries.ITEM.get(identifier)
        .map(reference -> reference.value())
        .orElse(Items.AIR);

    return item instanceof BlockItem;
  }

  private static void clearBattleCards(ServerPlayer player) {
    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
      if (isBattleCard(player.getInventory().getItem(slot))) {
        player.getInventory().setItem(slot, ItemStack.EMPTY);
      }
    }

    if (isBattleCard(player.containerMenu.getCarried())) {
      player.containerMenu.setCarried(ItemStack.EMPTY);
    }
  }
}
