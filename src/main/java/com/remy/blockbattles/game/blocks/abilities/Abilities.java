package com.remy.blockbattles.game.blocks.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class Abilities {
  public static void tntAbility(Level level, BlockPos pos) {
    if (level.isClientSide()) {
      return;
    }

    level.setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.south(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
  }
}
