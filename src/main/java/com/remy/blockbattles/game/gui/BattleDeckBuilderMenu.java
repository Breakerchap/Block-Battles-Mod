package com.remy.blockbattles.game.gui;

import java.util.ArrayList;
import java.util.List;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.logic.GameLogic;
import com.remy.blockbattles.game.logic.TeamSide;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BattleDeckBuilderMenu extends ChestMenu {
  private static final int ROW_COUNT = 6;
  private static final int MENU_SIZE = ROW_COUNT * 9;
  private static final int SELECTED_SLOT_COUNT = BattleCardItems.DECK_SIZE;
  private static final int AVAILABLE_SLOT_START = 18;
  private static final int PAGE_SIZE = MENU_SIZE - AVAILABLE_SLOT_START;
  private static final int SUMMARY_SLOT = 12;
  private static final int CLEAR_SLOT = 13;
  private static final int TEAM_SLOT = 14;
  private static final int PAGE_SLOT = 15;
  private static final int PREVIOUS_PAGE_SLOT = 16;
  private static final int NEXT_PAGE_SLOT = 17;

  private final GameLogic gameLogic;
  private final TeamSide teamSide;
  private final SimpleContainer container;
  private final ArrayList<BattleBlock> selectedDeck = new ArrayList<>();
  private final List<BattleBlock> availableBlocks = CreateBlocks.ALL;

  private int page;

  public BattleDeckBuilderMenu(int containerId, Inventory inventory, GameLogic gameLogic, TeamSide teamSide) {
    this(containerId, inventory, new SimpleContainer(MENU_SIZE), gameLogic, teamSide);
  }

  private BattleDeckBuilderMenu(
      int containerId,
      Inventory inventory,
      SimpleContainer container,
      GameLogic gameLogic,
      TeamSide teamSide) {
    super(MenuType.GENERIC_9x6, containerId, inventory, container, ROW_COUNT);
    this.container = container;
    this.gameLogic = gameLogic;
    this.teamSide = teamSide;

    selectedDeck.addAll(gameLogic.getBattleState().getConfiguredDeck(teamSide));
    rebuildMenu();
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public void clicked(int slotIndex, int button, ContainerInput clickType, Player player) {
    if (slotIndex < 0) {
      return;
    }

    if (slotIndex < SELECTED_SLOT_COUNT) {
      if (slotIndex < selectedDeck.size()) {
        selectedDeck.remove(slotIndex);
        rebuildMenu();
      }

      return;
    }

    if (slotIndex == CLEAR_SLOT) {
      selectedDeck.clear();
      rebuildMenu();
      return;
    }

    if (slotIndex == PREVIOUS_PAGE_SLOT) {
      if (page > 0) {
        page--;
        rebuildMenu();
      }

      return;
    }

    if (slotIndex == NEXT_PAGE_SLOT) {
      if (hasNextPage()) {
        page++;
        rebuildMenu();
      }

      return;
    }

    if (slotIndex >= AVAILABLE_SLOT_START && slotIndex < MENU_SIZE) {
      int blockIndex = page * PAGE_SIZE + (slotIndex - AVAILABLE_SLOT_START);

      if (blockIndex >= availableBlocks.size()) {
        return;
      }

      BattleBlock battleBlock = availableBlocks.get(blockIndex);

      if (!BattleCardItems.isSelectableForDeck(battleBlock)) {
        player.sendSystemMessage(Component.literal(BattleCardItems.getDeckRestrictionMessage(battleBlock)));
        return;
      }

      if (selectedDeck.size() >= BattleCardItems.DECK_SIZE) {
        player.sendSystemMessage(Component.literal("Your deck already has 12 cards."));
        return;
      }

      selectedDeck.add(battleBlock);
      rebuildMenu();
    }
  }

  @Override
  public void removed(Player player) {
    super.removed(player);

    if (selectedDeck.size() != BattleCardItems.DECK_SIZE) {
      player
          .sendSystemMessage(Component.literal("Pick exactly 12 blocks before closing if you want to save this deck."));
      return;
    }

    gameLogic.updateConfiguredDeck(teamSide, List.copyOf(selectedDeck));

    if (player instanceof ServerPlayer serverPlayer) {
      gameLogic.syncBattleHands(serverPlayer.level().getServer());
      BattleScoreboards.updateScoreboard(serverPlayer.level().getServer(), gameLogic.getBattleState());
    }

    player.sendSystemMessage(Component.literal(teamSide.getDisplayName() + " deck saved."));
  }

  private void rebuildMenu() {
    page = Math.min(page, Math.max(0, getTotalPages() - 1));
    container.clearContent();

    for (int slot = 0; slot < SELECTED_SLOT_COUNT; slot++) {
      container.setItem(slot, slot < selectedDeck.size()
          ? BattleCardItems.createDeckDisplayItem(selectedDeck.get(slot))
          : createDeckSlotPlaceholder(slot));
    }

    container.setItem(SUMMARY_SLOT, BattleCardItems.createNamedItem(
        Items.BOOK,
        Component.literal("Selected Deck").withStyle(ChatFormatting.GOLD),
        List.of(
            Component.literal(selectedDeck.size() + " / 12 cards").withStyle(ChatFormatting.YELLOW),
            Component.literal("Click a block below to add it.").withStyle(ChatFormatting.GRAY),
            Component.literal("Click a pinned card to remove it.").withStyle(ChatFormatting.GRAY))));

    container.setItem(CLEAR_SLOT, BattleCardItems.createNamedItem(
        Items.BARRIER,
        Component.literal("Clear Deck").withStyle(ChatFormatting.RED),
        List.of(Component.literal("Remove every selected card.").withStyle(ChatFormatting.GRAY))));

    container.setItem(TEAM_SLOT, BattleCardItems.createNamedItem(
        Items.NAME_TAG,
        Component.literal(teamSide.getDisplayName() + " Team Deck")
            .withStyle(teamSide == TeamSide.RED ? ChatFormatting.RED : ChatFormatting.BLUE),
        List.of(Component.literal("This deck is shared by your whole team.").withStyle(ChatFormatting.GRAY))));

    container.setItem(PAGE_SLOT, BattleCardItems.createNamedItem(
        Items.COMPASS,
        Component.literal("Page " + (page + 1) + " / " + getTotalPages()).withStyle(ChatFormatting.AQUA),
        List.of(Component.literal("Browse all battle blocks here.").withStyle(ChatFormatting.GRAY))));

    container.setItem(PREVIOUS_PAGE_SLOT, BattleCardItems.createNamedItem(
        Items.ARROW,
        Component.literal("Previous Page").withStyle(ChatFormatting.YELLOW),
        List.of(Component.literal(page > 0 ? "Go to the previous page." : "You are already on the first page.")
            .withStyle(ChatFormatting.GRAY))));

    container.setItem(NEXT_PAGE_SLOT, BattleCardItems.createNamedItem(
        Items.ARROW,
        Component.literal("Next Page").withStyle(ChatFormatting.YELLOW),
        List.of(Component.literal(hasNextPage() ? "Go to the next page." : "You are already on the last page.")
            .withStyle(ChatFormatting.GRAY))));

    for (int slot = AVAILABLE_SLOT_START; slot < MENU_SIZE; slot++) {
      int blockIndex = page * PAGE_SIZE + (slot - AVAILABLE_SLOT_START);

      if (blockIndex >= availableBlocks.size()) {
        container.setItem(slot, ItemStack.EMPTY);
        continue;
      }

      container.setItem(slot, BattleCardItems.createDeckDisplayItem(availableBlocks.get(blockIndex)));
    }

    container.setChanged();
    broadcastFullState();
  }

  private ItemStack createDeckSlotPlaceholder(int slot) {
    return BattleCardItems.createNamedItem(
        Items.STAINED_GLASS_PANE.pick(DyeColor.LIGHT_GRAY),
        Component.literal("Deck Slot " + (slot + 1)).withStyle(ChatFormatting.DARK_GRAY),
        List.of(Component.literal("Choose 12 blocks to save this deck.").withStyle(ChatFormatting.GRAY)));
  }

  private boolean hasNextPage() {
    return page + 1 < getTotalPages();
  }

  private int getTotalPages() {
    return Math.max(1, (availableBlocks.size() + PAGE_SIZE - 1) / PAGE_SIZE);
  }
}
