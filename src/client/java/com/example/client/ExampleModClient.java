package com.example.client;

import com.remy.blockbattles.client.BattleBlockOutlineRenderer;

import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BattleBlockOutlineRenderer.initialize();
	}
}
