package com.remy.blockbattles.mixin;

import com.remy.blockbattles.BlockBattlesMod;
import com.remy.blockbattles.game.gui.BattleScoreboards;
import com.remy.blockbattles.game.logic.BattlePlayerTeams;
import com.remy.blockbattles.game.logic.TeamSide;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
  @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
  private void blockBattles$beforePlaceBlock(
      BlockPlaceContext context,
      BlockState state,
      CallbackInfoReturnable<Boolean> cir) {

    if (!(context.getLevel() instanceof ServerLevel)) {
      return;
    }

    TeamSide teamSide = BattlePlayerTeams.getTeamSide(context.getPlayer()).orElse(null);
    String blockIdString = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

    if (BlockBattlesMod.GAME_LOGIC.canPlaceBattleBlock(blockIdString, teamSide)) {
      return;
    }

    if (context.getPlayer() != null) {
      Component message = teamSide == null
          ? Component.literal("Join the Red or Blue team first with /BB join red or /BB join blue.")
          : Component.literal("It is " + BlockBattlesMod.BATTLE_STATE.getActiveSide().getDisplayName() + "'s turn.");
      context.getPlayer().sendSystemMessage(message);
    }

    cir.setReturnValue(false);
  }

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
        pos,
        BattlePlayerTeams.getTeamSide(context.getPlayer()).orElse(null));

    if (handled) {
      BattleScoreboards.updateScoreboard(
          serverLevel.getServer(),
          BlockBattlesMod.BATTLE_STATE);
    }
  }
}
