package com.remy.blockbattles.client;

import net.fabricmc.api.ClientModInitializer;

public final class BlockBattlesClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    BattleBlockOutlineRenderer.initialize();
  }
}
