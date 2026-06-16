package com.remy.blockbattles.game.logic;

import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

public record PlacedBattleBlock(ServerLevel level, BlockPos pos, BattleBlock battleBlock) {
  public PlacedBattleBlock {
    level = Objects.requireNonNull(level, "level");
    pos = Objects.requireNonNull(pos, "pos").immutable();
    battleBlock = Objects.requireNonNull(battleBlock, "battleBlock");
  }

  public boolean stillExists() {
    String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    return blockId.equals(battleBlock.id.getId());
  }
}
