package com.remy.blockbattles.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.remy.blockbattles.BlockBattlesMod;
import com.remy.blockbattles.game.gui.BattleScoreboards;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;

@Mixin(ServerPlayerGameMode.class)
public class BreakBlockMixin {
  @Shadow
  protected ServerLevel level;

  @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
  private void blockBattles$beforeDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (!BlockBattlesMod.GAME_LOGIC.isGameRunning()) {
      return;
    }

    if (!BlockBattlesMod.GAME_LOGIC.canBreakBattleBlock(level, pos)) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "destroyBlock", at = @At("RETURN"))
  private void blockBattles$afterDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (!cir.getReturnValue()) {
      return;
    }

    if (!BlockBattlesMod.GAME_LOGIC.isGameRunning()) {
      return;
    }

    BlockBattlesMod.GAME_LOGIC.onBattleBlockBroken(level, pos);
    BlockBattlesMod.GAME_LOGIC.onAnyBlockBroken(level, pos);
    BlockBattlesMod.GAME_LOGIC.syncTrackedBattleBlocks(level.getServer());
    BattleScoreboards.updateScoreboard(level.getServer(), BlockBattlesMod.BATTLE_STATE);
  }
}
