package com.remy.blockbattles;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class BattleItems {
  public static final Item ENCYCLOPEDIA = register(
      "encyclopedia",
      new Item(createProperties("encyclopedia").stacksTo(1)));

  private BattleItems() {
  }

  public static void initialize() {
  }

  private static Item register(String path, Item item) {
    return Registry.register(
        BuiltInRegistries.ITEM,
        Identifier.fromNamespaceAndPath(BlockBattlesMod.MOD_ID, path),
        item);
  }

  private static Item.Properties createProperties(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(BlockBattlesMod.MOD_ID, path);
    return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
  }
}
