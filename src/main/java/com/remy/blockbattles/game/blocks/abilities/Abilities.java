package com.remy.blockbattles.game.blocks.abilities;

import com.remy.blockbattles.game.blocks.BattleBlock;
import com.remy.blockbattles.game.logic.BattleTeam;
import com.remy.blockbattles.game.logic.TeamSide;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public class Abilities {
  public static void tntAbility(Level level, BlockPos pos, TeamSide actingSide) {
    if (level.isClientSide()) {
      return;
    }

    level.setBlock(pos.north(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.south(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos.west(), Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
  }

  public static void redTulipAbility(BattleTeam actingTeam) {
    actingTeam.increaseMaxHealth(20);
  }

  public static void cherryLeavesAbility(BattleTeam actingTeam) {
    actingTeam.heal(actingTeam.getMaxHealth() / 10);
  }

  public static void cornflowerAbility(BattleTeam actingTeam) {
    actingTeam.heal(actingTeam.getHealth() / 4);
  }

  public static boolean growBlockUpwardIfAir(ServerLevel level, BlockPos pos) {
    BlockPos abovePos = pos.above();

    if (!level.getBlockState(abovePos).isAir()) {
      return false;
    }

    return level.setBlock(abovePos, level.getBlockState(pos), 3);
  }
}
