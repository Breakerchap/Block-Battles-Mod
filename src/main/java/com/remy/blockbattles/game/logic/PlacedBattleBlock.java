package com.remy.blockbattles.game.logic;

import java.util.Objects;

import com.remy.blockbattles.game.blocks.BattleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;

public final class PlacedBattleBlock {
  private final ServerLevel level;
  private BlockPos pos;
  private final BattleBlock battleBlock;
  private int ownerTurnCount;

  public PlacedBattleBlock(ServerLevel level, BlockPos pos, BattleBlock battleBlock) {
    this.level = Objects.requireNonNull(level, "level");
    this.pos = Objects.requireNonNull(pos, "pos").immutable();
    this.battleBlock = Objects.requireNonNull(battleBlock, "battleBlock");
  }

  public ServerLevel level() {
    return level;
  }

  public BlockPos pos() {
    return pos;
  }

  public BattleBlock battleBlock() {
    return battleBlock;
  }

  public boolean stillExists() {
    String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    return blockId.equals(battleBlock.id.getId());
  }

  public int advanceOwnerTurnCount() {
    ownerTurnCount++;
    return ownerTurnCount;
  }

  public void moveTo(BlockPos newPos) {
    pos = Objects.requireNonNull(newPos, "newPos").immutable();
  }
}
