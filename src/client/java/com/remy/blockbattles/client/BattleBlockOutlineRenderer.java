package com.remy.blockbattles.client;

import java.util.List;

import com.remy.blockbattles.game.logic.TeamSide;
import com.remy.blockbattles.network.BattleBlockOutlinePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

public final class BattleBlockOutlineRenderer {
  private static final float OUTLINE_EXPANSION = 0.001F;
  private static final float OUTLINE_WIDTH = 6.0F;

  private static List<BattleBlockOutlinePayload.OutlinedBlock> outlinedBlocks = List.of();

  private BattleBlockOutlineRenderer() {
  }

  public static void initialize() {
    ClientPlayNetworking.registerGlobalReceiver(BattleBlockOutlinePayload.TYPE,
        (payload, context) -> outlinedBlocks = payload.blocks());
    ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> outlinedBlocks = List.of());
    LevelRenderEvents.BEFORE_GIZMOS.register(context -> renderOutlines());
  }

  private static void renderOutlines() {
    Minecraft client = Minecraft.getInstance();

    if (client.level == null || outlinedBlocks.isEmpty()) {
      return;
    }

    String currentDimensionId = client.level.dimension().identifier().toString();

    try (var ignored = client.levelRenderer.collectPerFrameRenderThreadGizmos()) {
      for (BattleBlockOutlinePayload.OutlinedBlock outlinedBlock : outlinedBlocks) {
        if (!currentDimensionId.equals(outlinedBlock.dimensionId())) {
          continue;
        }

        Gizmos.cuboid(
            outlinedBlock.pos(),
            OUTLINE_EXPANSION,
            GizmoStyle.stroke(getOutlineColor(outlinedBlock.ownerSide()), OUTLINE_WIDTH));
      }
    }
  }

  private static int getOutlineColor(TeamSide side) {
    return switch (side) {
      case RED -> 0xFFFF5A5A;
      case BLUE -> 0xFF5AA8FF;
    };
  }
}
