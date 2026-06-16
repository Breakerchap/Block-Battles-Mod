package com.remy.blockbattles.mixin;

import com.remy.blockbattles.BlockBattlesMod;
import com.remy.blockbattles.game.gui.BattleScoreboards;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class PlaceBlockMixin {
  @Inject(method = "placeBlock", at = @At("RETURN"))
  private void blockBattles$afterPlaceBlock(
      BlockPlaceContext context,
      BlockState state,
      CallbackInfoReturnable<Boolean> cir) {

    if (!cir.getReturnValue()) {
      return;
    }

    if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }

    BlockPos pos = context.getClickedPos();

    var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
    String blockIdString = blockId.toString();

    boolean handled = BlockBattlesMod.GAME_LOGIC.onPlaceBattleBlock(
        blockIdString,
        serverLevel,
        pos);

    if (handled) {
      BattleScoreboards.updateScoreboard(
          serverLevel.getServer(),
          BlockBattlesMod.BATTLE_STATE);
    }
  }
}