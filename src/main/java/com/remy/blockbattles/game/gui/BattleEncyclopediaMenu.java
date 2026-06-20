package com.remy.blockbattles.game.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.blocks.Classification;
import com.remy.blockbattles.game.blocks.CreateBlocks;
import com.remy.blockbattles.game.logic.BattleWarp;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BattleEncyclopediaMenu extends ChestMenu {
  private static final int ROW_COUNT = 6;
  private static final int MENU_SIZE = ROW_COUNT * 9;
  private static final int CONTENT_SLOT_START = 9;
  private static final int PAGE_SIZE = MENU_SIZE - CONTENT_SLOT_START;
  private static final int BLOCKS_TAB_SLOT = 0;
  private static final int WARPS_TAB_SLOT = 1;
  private static final int SUMMARY_SLOT = 4;
  private static final int PREVIOUS_PAGE_SLOT = 7;
  private static final int NEXT_PAGE_SLOT = 8;

  private static final List<BattleBlock> BLOCK_ENTRIES = CreateBlocks.ALL;
  private static final List<WarpEntry> WARP_ENTRIES = List.of(
      new WarpEntry(
          BattleWarp.NIGHT,
          Items.CREEPER_HEAD,
          ChatFormatting.DARK_BLUE,
          "No",
          "Healing is prevented, mob head abilities are doubled, and there is no sunlight.",
          "Grass plus any two of Creeper Head, Zombie Head, and Skeleton Skull."),
      new WarpEntry(
          BattleWarp.VILLAGE_HOUSE,
          Items.CAKE,
          ChatFormatting.GOLD,
          "Yes",
          "Both teams heal 16 each turn and all mob head block effects are disabled.",
          "Red Bed next to village blocks like Carpet, Cake, Jungle Log, or Bookshelf."),
      new WarpEntry(
          BattleWarp.END,
          Items.ENDER_EYE,
          ChatFormatting.LIGHT_PURPLE,
          "No",
          "Beds explode on placement. If the warp lasts for 3 rounds, the lower-health team dies.",
          "Dragon Head, Shulker Box, or Dragon Egg on top of Endstone."),
      new WarpEntry(
          BattleWarp.LIBRARY,
          Items.BOOKSHELF,
          ChatFormatting.AQUA,
          "No",
          "Your hand becomes your entire remaining deck every turn.",
          "Bookshelf, Chiseled Bookshelf, Lectern, and Candle combinations."),
      new WarpEntry(
          BattleWarp.DEEP_DARK,
          Items.SCULK_SHRIEKER,
          ChatFormatting.DARK_AQUA,
          "No",
          "Each turn you permanently lose a deck card or take 30 damage if none remain.",
          "Two different Deep Dark blocks touching, like Sculk Sensor plus Sculk."),
      new WarpEntry(
          BattleWarp.SOUL_SAND_VALLEY,
          Items.SOUL_LANTERN,
          ChatFormatting.BLUE,
          "No",
          "Per-turn blocks are disabled and damage ignores defence.",
          "Soul Torch, Soul Lantern, or Soul Campfire placed on Soul Sand."),
      new WarpEntry(
          BattleWarp.BLIZZARD,
          Items.SNOW_BLOCK,
          ChatFormatting.WHITE,
          "No",
          "Turns scale up each round and Water freezes into Ice.",
          "Snow or Powdered Snow with more snow nearby, or Lightning Rod on top."),
      new WarpEntry(
          BattleWarp.OCEAN,
          Items.CONDUIT,
          ChatFormatting.AQUA,
          "Yes",
          "Explosions are disabled, fire blocks are removed, and Grass becomes Farmland.",
          "Water with Coral/Prismarine, or clustered Water."),
      new WarpEntry(
          BattleWarp.REDSTONE,
          Items.REDSTONE_TORCH,
          ChatFormatting.RED,
          "Yes",
          "Blocks placed during the warp activate twice.",
          "Redstone Torch or Repeater on top of Block of Redstone."),
      new WarpEntry(
          BattleWarp.NETHER_STRIP_MINE,
          Items.ANCIENT_DEBRIS,
          ChatFormatting.DARK_RED,
          "No",
          "Lava is stronger, Coral is suppressed, Water/Snow vanish, beds explode, all blocks are breakable, and both teams draw extra.",
          "Touching Nether ore blocks like Nether Gold Ore, Nether Quartz Ore, and Ancient Debris."),
      new WarpEntry(
          BattleWarp.NETHER,
          Items.LAVA_BUCKET,
          ChatFormatting.RED,
          "No",
          "Lava is stronger, Coral is suppressed, Water/Snow vanish, and beds explode.",
          "Lava, Magma Block, or Campfire on Obsidian or Netherrack."),
      new WarpEntry(
          BattleWarp.LUSH_CAVE,
          Items.MOSS_BLOCK,
          ChatFormatting.GREEN,
          "No",
          "Healing also damages the opponent for the same amount.",
          "Moss Block or Farmland on Deepslate or its ores."),
      new WarpEntry(
          BattleWarp.DESERT,
          Items.CACTUS,
          ChatFormatting.YELLOW,
          "Yes",
          "Teams with less than 2 defence take 20 damage at their turn start.",
          "Dead Bush/Cactus on Sand, or Sand next to sandstone variants."),
      new WarpEntry(
          BattleWarp.STRIP_MINE,
          Items.DEEPSLATE,
          ChatFormatting.GRAY,
          "Yes",
          "All blocks are breakable and both teams draw extra cards.",
          "Touching strip-mine cave blocks like Deepslate and its ores."),
      new WarpEntry(
          BattleWarp.BED_WARS,
          Items.OAK_PLANKS,
          ChatFormatting.RED,
          "Yes",
          "Special bed-fortification blocks get bonuses and beds can revive their owner.",
          "Red Bed with a strong local defence layout, or End Stone with Oak Planks."),
      new WarpEntry(
          BattleWarp.SWAMP,
          Items.SLIME_BALL,
          ChatFormatting.DARK_GREEN,
          "Yes",
          "The active player takes 3 damage at the start of their turn.",
          "Currently only available by setting the warp through code/debug state."),
      new WarpEntry(
          BattleWarp.DRIPSTONE_CAVE,
          Items.POINTED_DRIPSTONE,
          ChatFormatting.GOLD,
          "Yes",
          "All blocks become breakable and all damage is doubled.",
          "Pointed Dripstone next to Deepslate or Deepslate ores."),
      new WarpEntry(
          BattleWarp.NETHER_FORTRESS,
          Items.NETHER_BRICKS,
          ChatFormatting.DARK_RED,
          "No",
          "Nether rules apply, Nether Bricks become unbreakable, and some Nether Spawner stats are boosted.",
          "Spawner on Nether Bricks, or Lava touching Nether Bricks."),
      new WarpEntry(
          BattleWarp.BASTION,
          Items.PIGLIN_HEAD,
          ChatFormatting.GOLD,
          "No",
          "Defence damage is doubled and blocks next to gold have doubled effects.",
          "Piglin Head on Spawner/Polished Blackstone Bricks, Gold on Polished Blackstone Bricks, or Blackstone near Nether Gold Ore."),
      new WarpEntry(
          BattleWarp.FLOWER_FOREST,
          Items.RED_TULIP,
          ChatFormatting.LIGHT_PURPLE,
          "No",
          "Both teams gain +70 max health while the warp is active, and lose it when it ends.",
          "A flower on Grass with nearby Grass support."),
      new WarpEntry(
          BattleWarp.CAVE,
          Items.TORCH,
          ChatFormatting.YELLOW,
          "No",
          "All blocks become breakable and defence damage is halved.",
          "Torch on Deepslate, or Reinforced Deepslate on a Deepslate ore."),
      new WarpEntry(
          BattleWarp.MESA,
          Items.RED_SAND,
          ChatFormatting.GOLD,
          "Yes",
          "Defence gains and defence damage are doubled.",
          "Dead Bush/Cactus on Red Sand, or Red Sand next to red sandstone variants."),
      new WarpEntry(
          BattleWarp.PALE_GARDEN,
          Items.PALE_OAK_LOG,
          ChatFormatting.GRAY,
          "No",
          "Healing is inverted into damage.",
          "Creaking Heart with Pale Oak Log above and below."));

  private final SimpleContainer container;
  private Tab currentTab = Tab.BLOCKS;
  private int page;

  public BattleEncyclopediaMenu(int containerId, Inventory inventory) {
    this(containerId, inventory, new SimpleContainer(MENU_SIZE));
  }

  private BattleEncyclopediaMenu(int containerId, Inventory inventory, SimpleContainer container) {
    super(MenuType.GENERIC_9x6, containerId, inventory, container, ROW_COUNT);
    this.container = container;
    rebuildMenu();
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }

  @Override
  public void clicked(int slotIndex, int button, ContainerInput clickType, Player player) {
    if (slotIndex < 0 || slotIndex >= MENU_SIZE) {
      return;
    }

    if (slotIndex == BLOCKS_TAB_SLOT && currentTab != Tab.BLOCKS) {
      currentTab = Tab.BLOCKS;
      page = 0;
      rebuildMenu();
      return;
    }

    if (slotIndex == WARPS_TAB_SLOT && currentTab != Tab.WARPS) {
      currentTab = Tab.WARPS;
      page = 0;
      rebuildMenu();
      return;
    }

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

    container.setItem(BLOCKS_TAB_SLOT, createTabItem(
        Items.GRASS_BLOCK,
        Component.literal("Blocks").withStyle(currentTab == Tab.BLOCKS ? ChatFormatting.GREEN : ChatFormatting.GRAY),
        currentTab == Tab.BLOCKS
            ? "You are viewing every battle block."
            : "Switch to the block encyclopedia."));

    container.setItem(WARPS_TAB_SLOT, createTabItem(
        Items.NETHER_STAR,
        Component.literal("Warps").withStyle(currentTab == Tab.WARPS ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
        currentTab == Tab.WARPS
            ? "You are viewing every active warp."
            : "Switch to the warp encyclopedia."));

    container.setItem(SUMMARY_SLOT, BattleCardItems.createNamedItem(
        Items.BOOK,
        Component.literal("Block Battles Encyclopedia").withStyle(ChatFormatting.GOLD),
        createSummaryLore()));

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
      int entryIndex = page * PAGE_SIZE + (slot - CONTENT_SLOT_START);

      if (currentTab == Tab.BLOCKS) {
        if (entryIndex >= BLOCK_ENTRIES.size()) {
          container.setItem(slot, ItemStack.EMPTY);
          continue;
        }

        container.setItem(slot, createBlockEntryItem(BLOCK_ENTRIES.get(entryIndex)));
        continue;
      }

      if (entryIndex >= WARP_ENTRIES.size()) {
        container.setItem(slot, ItemStack.EMPTY);
        continue;
      }

      container.setItem(slot, createWarpEntryItem(WARP_ENTRIES.get(entryIndex)));
    }

    container.setChanged();
    broadcastFullState();
  }

  private ItemStack createTabItem(Item icon, Component name, String description) {
    return BattleCardItems.createNamedItem(
        icon,
        name,
        List.of(Component.literal(description).withStyle(ChatFormatting.GRAY)));
  }

  private List<Component> createSummaryLore() {
    ArrayList<Component> lore = new ArrayList<>();
    lore.add(Component.literal("Section: ").withStyle(ChatFormatting.GRAY)
        .append(Component.literal(currentTab == Tab.BLOCKS ? "Blocks" : "Warps")
            .withStyle(currentTab == Tab.BLOCKS ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE)));
    lore.add(Component.literal("Entries: ").withStyle(ChatFormatting.GRAY)
        .append(Component.literal(String.valueOf(currentTab == Tab.BLOCKS ? BLOCK_ENTRIES.size() : WARP_ENTRIES.size()))
            .withStyle(ChatFormatting.YELLOW)));
    lore.add(Component.literal("Page: ").withStyle(ChatFormatting.GRAY)
        .append(Component.literal((page + 1) + " / " + getTotalPages()).withStyle(ChatFormatting.AQUA)));
    lore.add(Component.empty());
    lore.add(Component.literal("Hover an entry to read all of its details.").withStyle(ChatFormatting.GRAY));
    lore.add(Component.literal("Use this anytime, even outside an active game.").withStyle(ChatFormatting.DARK_GRAY));
    return lore;
  }

  private ItemStack createBlockEntryItem(BattleBlock battleBlock) {
    ArrayList<Component> lore = new ArrayList<>();
    BattleCompletionTracker.BlockCompletionStatus trackerStatus = BattleCompletionTracker.getBlockStatus(battleBlock.displayName);

    lore.add(labeledLine("ID", battleBlock.id.getId(), ChatFormatting.DARK_GRAY, ChatFormatting.GRAY));
    lore.add(labeledLine(
        "Classification",
        formatClassification(battleBlock.classification),
        ChatFormatting.GRAY,
        ChatFormatting.LIGHT_PURPLE));
    lore.add(labeledLine("Implementation", trackerStatus.overallStatus(), ChatFormatting.GRAY, statusColor(trackerStatus.overallStatus())));
    lore.add(labeledLine("Ability Status", trackerStatus.ability(), ChatFormatting.DARK_GRAY, statusColor(trackerStatus.ability())));
    lore.add(labeledLine("Requirement Status", trackerStatus.requirements(), ChatFormatting.DARK_GRAY, statusColor(trackerStatus.requirements())));
    lore.add(labeledLine("Combo Status", trackerStatus.combos(), ChatFormatting.DARK_GRAY, statusColor(trackerStatus.combos())));
    lore.add(labeledLine(
        "Deck",
        BattleCardItems.isSelectableForDeck(battleBlock) ? "Selectable" : "Unchoosable",
        ChatFormatting.GRAY,
        BattleCardItems.isSelectableForDeck(battleBlock) ? ChatFormatting.GREEN : ChatFormatting.RED));
    lore.add(Component.empty());
    addWrappedLabeledText(lore, "Ability", battleBlock.abilityDescription, ChatFormatting.AQUA, ChatFormatting.WHITE);

    if (battleBlock.hasPlacementRequirements()) {
      addWrappedLabeledText(lore, "Requirements", battleBlock.requirementDescription, ChatFormatting.YELLOW, ChatFormatting.WHITE);
    }

    addWrappedLabeledText(lore, "Combos", getBlockCombosText(battleBlock), ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE);

    if (!trackerStatus.notes().isBlank()) {
      addWrappedLabeledText(lore, "Tracker Note", trackerStatus.notes(), ChatFormatting.GOLD, ChatFormatting.GRAY);
    }

    lore.add(Component.empty());
    lore.add(statLine("Damage", battleBlock.damage, battleBlock.damagePerTurn, ChatFormatting.RED));
    lore.add(statLine("Defence", battleBlock.defence, battleBlock.defencePerTurn, ChatFormatting.AQUA));
    lore.add(statLine("Healing", battleBlock.healing, battleBlock.healingPerTurn, ChatFormatting.GREEN));
    lore.add(statLine("Defence Damage", battleBlock.defenceDamage, battleBlock.defenceDamagePerTurn, ChatFormatting.GOLD));

    if (!BattleCardItems.isSelectableForDeck(battleBlock)) {
      lore.add(Component.empty());
      addWrappedLabeledText(
          lore,
          "Note",
          BattleCardItems.getDeckRestrictionMessage(battleBlock),
          ChatFormatting.DARK_RED,
          ChatFormatting.RED);
    }

    return BattleCardItems.createNamedItem(
        BattleCardItems.resolveDisplayItem(battleBlock),
        Component.literal(battleBlock.displayName).withStyle(ChatFormatting.WHITE),
        lore);
  }

  private ItemStack createWarpEntryItem(WarpEntry entry) {
    ArrayList<Component> lore = new ArrayList<>();
    Identifier structureId = entry.warp().getStructureId();
    BattleCompletionTracker.WarpCompletionStatus trackerStatus = BattleCompletionTracker.getWarpStatus(entry.warp());

    lore.add(labeledLine("Implementation", trackerStatus.overallStatus(), ChatFormatting.GRAY, statusColor(trackerStatus.overallStatus())));
    lore.add(labeledLine("Effect Status", trackerStatus.effect(), ChatFormatting.DARK_GRAY, statusColor(trackerStatus.effect())));
    lore.add(labeledLine("Combo Status", trackerStatus.combos(), ChatFormatting.DARK_GRAY, statusColor(trackerStatus.combos())));
    lore.add(labeledLine("Sunlight", entry.sunlight(), ChatFormatting.GRAY, "Yes".equals(entry.sunlight()) ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA));

    if (structureId != null) {
      lore.add(labeledLine("Structure", structureId.toString(), ChatFormatting.DARK_GRAY, ChatFormatting.GRAY));
    }

    lore.add(Component.empty());
    addWrappedLabeledText(lore, "Effect", entry.effectText(), ChatFormatting.AQUA, ChatFormatting.WHITE);
    addWrappedLabeledText(lore, "Combos", entry.triggerText(), ChatFormatting.YELLOW, ChatFormatting.WHITE);

    if (!trackerStatus.notes().isBlank()) {
      addWrappedLabeledText(lore, "Tracker Note", trackerStatus.notes(), ChatFormatting.GOLD, ChatFormatting.GRAY);
    }

    return BattleCardItems.createNamedItem(
        entry.icon(),
        Component.literal(entry.warp().getDisplayName()).withStyle(entry.nameColor()),
        lore);
  }

  private Component labeledLine(String label, String value, ChatFormatting labelColor, ChatFormatting valueColor) {
    return Component.literal(label + ": ").withStyle(labelColor)
        .append(Component.literal(value).withStyle(valueColor));
  }

  private ChatFormatting statusColor(String status) {
    return switch (Objects.requireNonNullElse(status, "").trim().toLowerCase()) {
      case "complete", "done", "yes" -> ChatFormatting.GREEN;
      case "partial" -> ChatFormatting.GOLD;
      case "missing", "no" -> ChatFormatting.RED;
      case "n/a" -> ChatFormatting.GRAY;
      default -> ChatFormatting.WHITE;
    };
  }

  private void addWrappedLabeledText(
      List<Component> lore,
      String label,
      String text,
      ChatFormatting labelColor,
      ChatFormatting valueColor) {
    List<String> wrappedLines = wrapText(text, 34);

    if (wrappedLines.isEmpty()) {
      lore.add(labeledLine(label, "None", labelColor, valueColor));
      return;
    }

    lore.add(labeledLine(label, wrappedLines.get(0), labelColor, valueColor));

    for (int index = 1; index < wrappedLines.size(); index++) {
      lore.add(Component.literal("  " + wrappedLines.get(index)).withStyle(valueColor));
    }
  }

  private List<String> wrapText(String text, int maxLineLength) {
    ArrayList<String> lines = new ArrayList<>();
    String normalized = text == null ? "" : text.trim();

    if (normalized.isEmpty()) {
      return lines;
    }

    for (String paragraph : normalized.split("\\R")) {
      StringBuilder currentLine = new StringBuilder();

      for (String word : paragraph.split("\\s+")) {
        if (currentLine.length() == 0) {
          currentLine.append(word);
          continue;
        }

        if (currentLine.length() + 1 + word.length() > maxLineLength) {
          lines.add(currentLine.toString());
          currentLine.setLength(0);
          currentLine.append(word);
          continue;
        }

        currentLine.append(' ').append(word);
      }

      if (currentLine.length() > 0) {
        lines.add(currentLine.toString());
      }
    }

    return lines;
  }

  private Component statLine(String label, int value, boolean perTurn, ChatFormatting statColor) {
    String suffix = perTurn ? " / turn" : "";
    String displayValue = value > 0 ? "+" + value : String.valueOf(value);

    return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
        .append(Component.literal(displayValue + suffix).withStyle(statColor));
  }

  private String getBlockCombosText(BattleBlock battleBlock) {
    return switch (Objects.requireNonNull(battleBlock, "battleBlock").id) {
      case GRASS_BLOCK -> "Water turns it into Farmland. Mushroom Stem above it or nearby Mycelium turns it into Mycelium. Nearby Composter turns it into Podzol. Grass with flowers can trigger Flower Forest Warp.";
      case DIRT -> "Adjacent Grass, Podzol, or Mycelium converts it into that matching ground block.";
      case MYCELIUM -> "Spreads onto adjacent Dirt, and Grass with Mushroom Stem above can turn into Mycelium.";
      case DEAD_BUSH -> "On Sand it helps trigger Desert Warp. On Red Sand it helps trigger Mesa Warp.";
      case CACTUS -> "On Sand it helps trigger Desert Warp. On Red Sand it helps trigger Mesa Warp.";
      case CARVED_PUMPKIN -> "On Iron Block it grants extra shield and damage. On Snow it triggers the Snow combo burst and clears nearby mob heads.";
      case WATER -> "Next to Lava it becomes Obsidian. With Coral or Prismarine it helps trigger Ocean Warp.";
      case HORN_CORAL_BLOCK, TUBE_CORAL_BLOCK, BUBBLE_CORAL_BLOCK -> "Adjacent Water buffs this Coral. Together with Water and Prismarine it helps trigger Ocean Warp.";
      case PRISMARINE -> "Together with Water and Coral it helps trigger Ocean Warp. With Conduit it enables the revive combo.";
      case NETHERRACK -> "Crimson or Warped Hyphae above turns it into the matching Nylium. With Lava, Magma Block, or Campfire it helps trigger Nether Warp.";
      case NETHER_GOLD_ORE, NETHER_QUARTZ_ORE, ANCIENT_DEBRIS -> "Touching other Nether mining blocks helps trigger Nether Strip Mine Warp.";
      case CRIMSON_HYPHAE -> "Placed above Netherrack, it converts it into Crimson Nylium.";
      case WARPED_HYPHAE -> "Placed above Netherrack, it converts it into Warped Nylium.";
      case SOUL_SAND -> "Under Soul Torch, Soul Lantern, or Soul Campfire it triggers Soul Sand Valley Warp.";
      case MAGMA_BLOCK -> "Water above it gives extra healing. On Netherrack or Obsidian it helps trigger Nether Warp.";
      case SOUL_TORCH, SOUL_LANTERN, SOUL_CAMPFIRE -> "Placed on Soul Sand, it helps trigger Soul Sand Valley Warp.";
      case WITHER_ROSE -> "Turns nearby Grass into Soul Sand and cashes in extra damage and healing from those conversions.";
      case NETHER_BRICKS -> "With a Spawner above or Lava beside it, it helps trigger Nether Fortress Warp.";
      case RESPAWN_ANCHOR -> "With 2 adjacent Glowstone, it revives you in Nether-family warps or explodes outside them.";
      case END_STONE -> "Dragon Head, Shulker Box, or Dragon Egg on top helps trigger End Warp. In Bed Wars it also gains extra defence per turn.";
      case FURNACE -> "Adjacent Block of Coal gives +5 defence each turn. Adjacent Campfire gives +6 healing each turn.";
      case CAULDRON -> "Water above heals each turn. Lava above doubles Lava damage. Powdered Snow above doubles its delayed hit.";
      case COMPOSTER -> "Nearby Grass becomes Podzol. Adjacent Natural battle blocks are absorbed, stored, and activated for you.";
      case STONECUTTER -> "Adjacent Cave battle blocks are absorbed, stored, and activated for you.";
      case LECTERN, BOOKSHELF, CHISELED_BOOKSHELF, CANDLE -> "Bookshelf, Chiseled Bookshelf, Lectern, and Candle combinations can trigger Library Warp.";
      case ENCHANTING_TABLE -> "Nearby Bookshelf gives it extra healing when it activates.";
      case REDSTONE_TORCH, REPEATER -> "On top of Block of Redstone, it helps trigger Redstone Warp.";
      case TORCH -> "On top of Deepslate, it helps trigger Cave Warp.";
      case LIGHTNING_ROD -> "On top of Snow or Powdered Snow, it helps trigger Blizzard Warp.";
      case SPAWNER -> "On Nether Bricks it helps trigger Nether Fortress Warp. Piglin Head on top helps trigger Bastion Warp.";
      case SCULK_SENSOR, CALIBRATED_SCULK_SENSOR, SCULK_SHRIEKER, SCULK_CATALYST, SCULK -> "Touching another Deep Dark block helps trigger Deep Dark Warp.";
      case CONDUIT -> "Adjacent Water and Prismarine lets it revive you at half health.";
      case BEACON -> "Support block underneath changes its effect: Gold gives shield, Emerald gives extra draws, Iron reduces damage, Netherite protects nearby blocks.";
      case DEEPSLATE -> "Torch on top helps trigger Cave Warp. Moss Block or Farmland on top helps trigger Lush Cave. Together with Deepslate ores it helps trigger Strip Mine.";
      case REINFORCED_DEEPSLATE -> "On top of a Deepslate ore, it helps trigger Cave Warp.";
      case DEEPSLATE_GOLD_ORE, DEEPSLATE_REDSTONE_ORE -> "Together with Deepslate and other cave ore blocks they help trigger Strip Mine, Lush Cave, and Dripstone Cave combos.";
      case POLISHED_BLACKSTONE_BRICKS -> "Piglin Head or Gold Block on top of it can trigger Bastion Warp.";
      case GOLD_BLOCK -> "Adjacent Player Head turns nearby blocks into Gold. On Polished Blackstone Bricks it helps trigger Bastion. Under Beacon it powers the shield combo.";
      case IRON_BLOCK -> "Under Carved Pumpkin it grants bonus effects. Under Beacon it reduces incoming damage.";
      case EMERALD_BLOCK -> "Under Beacon it grants extra draws each turn.";
      case REDSTONE_BLOCK -> "Redstone Torch or Repeater on top helps trigger Redstone Warp. Next to a Lightning Rod-boosted TNT, Creeper Head, or Obsidian combo it powers the full board wipe.";
      case CHISELED_COPPER -> "Adjacent Honeycomb stops it from breaking after its activation.";
      case OBSIDIAN -> "Adjacent Lava turns Lava into Obsidian. It protects nearby blocks from explosions and supports Nether Warp or End Crystal placement. A Lightning Rod above it with adjacent Redstone Block powers the board-wipe combo.";
      case LAVA -> "Next to Water it becomes Obsidian. On Cauldron it deals double damage. On Netherrack or Obsidian it helps trigger Nether Warp.";
      case TNT -> "Breaks adjacent blocks on placement. A Lightning Rod above it enlarges the blast, and adding adjacent Redstone Block upgrades it into a full tracked-block wipe.";
      case POINTED_DRIPSTONE -> "Next to Deepslate ores it helps trigger Dripstone Cave Warp.";
      case RED_CARPET -> "Placed above a block, it doubles damage from that block's relevant effects.";
      case BLUE_CARPET -> "Placed above a block, it doubles defence gained from that block's relevant effects.";
      case GREEN_CARPET -> "Placed above a block, it doubles healing from that block's relevant effects.";
      case RED_BED -> "Village House and Bed Wars both use Red Bed combos. During End and Nether-family warps it explodes on placement.";
      case MUSHROOM_STEM -> "Above Grass it turns that Grass into Mycelium.";
      case SLIME_BLOCK -> "Adjacent Slime Blocks increase its per-turn damage.";
      case PLAYER_HEAD -> "Adjacent Gold Block turns nearby blocks into Gold.";
      case CREEPER_HEAD, ZOMBIE_HEAD, SKELETON_SKULL -> "These mob heads combine with each other and Grass to trigger Night Warp. Creeper Head also gets a bigger explosion with a Lightning Rod above it, and Redstone next to that setup wipes all tracked battle blocks.";
      case PIGLIN_HEAD -> "On Spawner or Polished Blackstone Bricks, it helps trigger Bastion Warp.";
      case DRAGON_HEAD, DRAGON_EGG, SHULKER_BOX -> "On top of Endstone they help trigger End Warp.";
      case OAK_PLANKS -> "Around Red Bed or next to End Stone they help trigger Bed Wars Warp.";
      case PALE_OAK_LOG, CREAKING_HEART -> "Pale Oak Log above and below Creaking Heart triggers Pale Garden Warp and supports log growth.";
      default -> "None.";
    };
  }

  private String formatClassification(Classification classification) {
    return switch (Objects.requireNonNull(classification, "classification")) {
      case NONE -> "None";
      case NATURAL -> "Natural";
      case MAN_MADE -> "Man-made";
      case CAVE -> "Cave";
      case OTHERWORLDLY -> "Otherworldly";
    };
  }

  private boolean hasNextPage() {
    return page + 1 < getTotalPages();
  }

  private int getTotalPages() {
    int entryCount = currentTab == Tab.BLOCKS ? BLOCK_ENTRIES.size() : WARP_ENTRIES.size();
    return Math.max(1, (entryCount + PAGE_SIZE - 1) / PAGE_SIZE);
  }

  private enum Tab {
    BLOCKS,
    WARPS
  }

  private record WarpEntry(
      BattleWarp warp,
      Item icon,
      ChatFormatting nameColor,
      String sunlight,
      String effectText,
      String triggerText) {
  }
}
