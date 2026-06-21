package com.remy.blockbattles.game.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.remy.blockbattles.game.blocks.BattleBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BattleAbilityChoiceMenu extends ChestMenu {
  private static final int ROW_COUNT = 6;
  private static final int MENU_SIZE = ROW_COUNT * 9;
  private static final int AVAILABLE_SLOT_START = 18;
  private static final int PAGE_SIZE = MENU_SIZE - AVAILABLE_SLOT_START;
  private static final int SUMMARY_SLOT = 12;
  private static final int PREVIOUS_PAGE_SLOT = 16;
  private static final int NEXT_PAGE_SLOT = 17;

  private final SimpleContainer container;
  private final List<BattleBlock> availableBlocks;
  private final int selectionLimit;
  private final Component summaryTitle;
  private final List<Component> summaryLore;
  private final BiConsumer<ServerPlayer, List<BattleBlock>> completionAction;
  private final Consumer<ServerPlayer> cancelAction;
  private final ArrayList<BattleBlock> selectedBlocks = new ArrayList<>();

  private boolean completed;
  private int page;

  public BattleAbilityChoiceMenu(
      int containerId,
      Inventory inventory,
      List<BattleBlock> availableBlocks,
      int selectionLimit,
      Component summaryTitle,
      List<Component> summaryLore,
      BiConsumer<ServerPlayer, List<BattleBlock>> completionAction) {
    this(
        containerId,
        inventory,
        availableBlocks,
        selectionLimit,
        summaryTitle,
        summaryLore,
        completionAction,
        null);
  }

  public BattleAbilityChoiceMenu(
      int containerId,
      Inventory inventory,
      List<BattleBlock> availableBlocks,
      int selectionLimit,
      Component summaryTitle,
      List<Component> summaryLore,
      BiConsumer<ServerPlayer, List<BattleBlock>> completionAction,
      Consumer<ServerPlayer> cancelAction) {
    this(
        containerId,
        inventory,
        new SimpleContainer(MENU_SIZE),
        availableBlocks,
        selectionLimit,
        summaryTitle,
        summaryLore,
        completionAction,
        cancelAction);
  }

  private BattleAbilityChoiceMenu(
      int containerId,
      Inventory inventory,
      SimpleContainer container,
      List<BattleBlock> availableBlocks,
      int selectionLimit,
      Component summaryTitle,
      List<Component> summaryLore,
      BiConsumer<ServerPlayer, List<BattleBlock>> completionAction,
      Consumer<ServerPlayer> cancelAction) {
    super(MenuType.GENERIC_9x6, containerId, inventory, container, ROW_COUNT);
    this.container = container;
    this.availableBlocks = List.copyOf(Objects.requireNonNull(availableBlocks, "availableBlocks"));
    this.selectionLimit = Math.max(1, selectionLimit);
    this.summaryTitle = Objects.requireNonNull(summaryTitle, "summaryTitle");
    this.summaryLore = List.copyOf(Objects.requireNonNull(summaryLore, "summaryLore"));
    this.completionAction = Objects.requireNonNull(completionAction, "completionAction");
    this.cancelAction = cancelAction;
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

    if (slotIndex < selectionLimit) {
      if (slotIndex < selectedBlocks.size()) {
        selectedBlocks.remove(slotIndex);
        rebuildMenu();
      }

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

    if (slotIndex < AVAILABLE_SLOT_START || slotIndex >= MENU_SIZE || completed) {
      return;
    }

    int blockIndex = page * PAGE_SIZE + (slotIndex - AVAILABLE_SLOT_START);

    if (blockIndex >= availableBlocks.size()) {
      return;
    }

    selectedBlocks.add(availableBlocks.get(blockIndex));

    if (selectedBlocks.size() >= selectionLimit && player instanceof ServerPlayer serverPlayer) {
      completeSelection(serverPlayer);
      return;
    }

    rebuildMenu();
  }

  @Override
  public void removed(Player player) {
    super.removed(player);

    if (!completed) {
      if (cancelAction != null && player instanceof ServerPlayer serverPlayer) {
        cancelAction.accept(serverPlayer);
        return;
      }

      player.sendSystemMessage(Component.literal("Choice cancelled."));
    }
  }

  private void completeSelection(ServerPlayer player) {
    if (completed) {
      return;
    }

    completed = true;
    completionAction.accept(player, List.copyOf(selectedBlocks));
    player.closeContainer();
  }

  private void rebuildMenu() {
    page = Math.min(page, Math.max(0, getTotalPages() - 1));
    container.clearContent();

    for (int slot = 0; slot < selectionLimit; slot++) {
      container.setItem(slot, slot < selectedBlocks.size()
          ? BattleCardItems.createChoiceDisplayItem(selectedBlocks.get(slot))
          : BattleCardItems.createNamedItem(
              Items.STAINED_GLASS_PANE.pick(net.minecraft.world.item.DyeColor.LIGHT_GRAY),
              Component.literal("Choice " + (slot + 1)).withStyle(ChatFormatting.DARK_GRAY),
              List.of(Component.literal("Click a block below to choose it.").withStyle(ChatFormatting.GRAY))));
    }

    ArrayList<Component> dynamicLore = new ArrayList<>(summaryLore);
    dynamicLore.add(Component.literal(selectedBlocks.size() + " / " + selectionLimit + " chosen").withStyle(ChatFormatting.YELLOW));
    dynamicLore.add(Component.literal("Click a chosen card to remove it.").withStyle(ChatFormatting.GRAY));

    container.setItem(SUMMARY_SLOT, BattleCardItems.createNamedItem(
        Items.BOOK,
        summaryTitle,
        dynamicLore));

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

      container.setItem(slot, BattleCardItems.createChoiceDisplayItem(availableBlocks.get(blockIndex)));
    }

    container.setChanged();
    broadcastFullState();
  }

  private boolean hasNextPage() {
    return page + 1 < getTotalPages();
  }

  private int getTotalPages() {
    return Math.max(1, (availableBlocks.size() + PAGE_SIZE - 1) / PAGE_SIZE);
  }
}
