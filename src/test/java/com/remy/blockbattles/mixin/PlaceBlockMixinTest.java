package com.remy.blockbattles.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class PlaceBlockMixinTest {
  @Test
  void resolveSupportPosUsesBlockBelowWhenPlacingOnTop() throws Exception {
    BlockPos placementPos = new BlockPos(4, 65, 7);

    assertEquals(
        new BlockPos(4, 64, 7),
        invokeResolveSupportPos(placementPos, Direction.UP, false));
  }

  @Test
  void resolveSupportPosUsesClickedBlockWhenPlacingOnSide() throws Exception {
    BlockPos placementPos = new BlockPos(11, 20, 31);

    assertEquals(
        new BlockPos(10, 20, 31),
        invokeResolveSupportPos(placementPos, Direction.EAST, false));
  }

  @Test
  void resolveSupportPosKeepsClickedPosWhenReplacingClickedBlock() throws Exception {
    BlockPos clickedPos = new BlockPos(2, 70, 2);

    assertEquals(
        clickedPos,
        invokeResolveSupportPos(clickedPos, Direction.UP, true));
  }

  private static BlockPos invokeResolveSupportPos(BlockPos clickedPos, Direction clickedFace, boolean replacingClickedOnBlock)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method method = PlaceBlockMixin.class.getDeclaredMethod(
        "blockBattles$resolveSupportPos",
        BlockPos.class,
        Direction.class,
        boolean.class);
    method.setAccessible(true);
    return (BlockPos) method.invoke(null, clickedPos, clickedFace, replacingClickedOnBlock);
  }
}
