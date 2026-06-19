package com.remy.blockbattles.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class PlaceBlockMixinTest {
  @Test
  void resolveSupportPosUsesBlockBelowWhenPlacingOnTop() {
    BlockPos placementPos = new BlockPos(4, 65, 7);

    assertEquals(
        new BlockPos(4, 64, 7),
        PlaceBlockMixin.blockBattles$resolveSupportPos(placementPos, Direction.UP, false));
  }

  @Test
  void resolveSupportPosUsesClickedBlockWhenPlacingOnSide() {
    BlockPos placementPos = new BlockPos(11, 20, 31);

    assertEquals(
        new BlockPos(10, 20, 31),
        PlaceBlockMixin.blockBattles$resolveSupportPos(placementPos, Direction.EAST, false));
  }

  @Test
  void resolveSupportPosKeepsClickedPosWhenReplacingClickedBlock() {
    BlockPos clickedPos = new BlockPos(2, 70, 2);

    assertEquals(
        clickedPos,
        PlaceBlockMixin.blockBattles$resolveSupportPos(clickedPos, Direction.UP, true));
  }
}
