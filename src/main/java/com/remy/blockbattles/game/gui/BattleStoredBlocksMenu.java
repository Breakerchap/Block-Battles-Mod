package com.remy.blockbattles.game.gui;

import java.util.List;
import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BattleStoredBlocksMenu extends ChestMenu {
  private static final int ROW_COUNT = 6;
  private static final int MENU_SIZE = ROW_COUNT * 9;
  private static final int CONTENT_SLOT_START = 18;
  private static final int PAGE_SIZE = MENU_SIZE - CONTENT_SLOT_START;
  private static final int SUMMARY_SLOT = 12;
  private static final int PREVIOUS_PAGE_SLOT = 16;
  private static final int NEXT_PAGE_SLOT = 17;

  private final SimpleContainer container;
  private final Component summaryTitle;
  private final List<BattleBlock> storedBlocks;

  private int page;

  public BattleStoredBlocksMenu(
      int containerId,
      Inventory inventory,
      Component summaryTitle,
      List<BattleBlock> storedBlocks) {
    this(containerId, inventory, new SimpleContainer(MENU_SIZE), summaryTitle, storedBlocks);
  }

  private BattleStoredBlocksMenu(
      int containerId,
      Inventory inventory,
      SimpleContainer container,
      Component summaryTitle,
      List<BattleBlock> storedBlocks) {
    super(MenuType.GENERIC_9x6, containerId, inventory, container, ROW_COUNT);
    this.container = container;
    this.summaryTitle = Objects.requireNonNull(summaryTitle, "summaryTitle");
    this.storedBlocks = List.copyOf(Objects.requireNonNull(storedBlocks, "storedBlocks"));
    rebuildMenu();
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public void clicked(int slotIndex, int button, ContainerInput clickType, Player player) {
    if (slotIndex == PREVIOUS_PAGE_SLOT && page > 0) {
      page--;
      rebuildMenu();
      return;
    }

    if (slotIndex == NEXT_PAGE_SLOT && hasNextPage()) {
      page++;
      rebuildMenu();
    }
  }

  private void rebuildMenu() {
    page = Math.min(page, Math.max(0, getTotalPages() - 1));
    container.clearContent();

    container.setItem(SUMMARY_SLOT, BattleCardItems.createNamedItem(
        Items.CHEST,
        summaryTitle,
        List.of(
            Component.literal(storedBlocks.size() + " stored blocks").withStyle(ChatFormatting.YELLOW),
            Component.literal("These blocks are persisted inside this battle block.").withStyle(ChatFormatting.GRAY))));

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

    for (int slot = CONTENT_SLOT_START; slot < MENU_SIZE; slot++) {
      int blockIndex = page * PAGE_SIZE + (slot - CONTENT_SLOT_START);

      if (blockIndex >= storedBlocks.size()) {
        container.setItem(slot, ItemStack.EMPTY);
        continue;
      }

      container.setItem(slot, BattleCardItems.createChoiceDisplayItem(storedBlocks.get(blockIndex)));
    }

    container.setChanged();
    broadcastFullState();
  }

  private boolean hasNextPage() {
    return page + 1 < getTotalPages();
  }

  private int getTotalPages() {
    return Math.max(1, (storedBlocks.size() + PAGE_SIZE - 1) / PAGE_SIZE);
  }
}
