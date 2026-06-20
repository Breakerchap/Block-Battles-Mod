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

    if (!BlockBattlesMod.GAME_LOGIC.isGameRunning()) {
      return;
    }

    TeamSide teamSide = BattlePlayerTeams.getTeamSide(context.getPlayer()).orElse(null);
    String blockIdString = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    BlockPos supportPos = blockBattles$resolveSupportPos(context);
    String supportBlockId = BuiltInRegistries.BLOCK.getKey(context.getLevel().getBlockState(supportPos).getBlock())
        .toString();
    TeamSide supportOwnerSide = BlockBattlesMod.GAME_LOGIC.getPlacedBattleBlockOwner(
        (ServerLevel) context.getLevel(),
        supportPos);

    String failureMessage = BlockBattlesMod.GAME_LOGIC.getBattleBlockPlacementFailure(
        blockIdString,
        teamSide,
        supportBlockId,
        supportOwnerSide);

    if (failureMessage == null) {
      return;
    }

    if (context.getPlayer() != null) {
      context.getPlayer().sendSystemMessage(Component.literal(failureMessage));
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

    if (!BlockBattlesMod.GAME_LOGIC.isGameRunning()) {
      return;
    }

    BlockPos pos = blockBattles$resolvePlacedPos(context, state);

    var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
    String blockIdString = blockId.toString();

    boolean handled = BlockBattlesMod.GAME_LOGIC.onPlaceBattleBlock(
        blockIdString,
        serverLevel,
        pos,
        BattlePlayerTeams.getTeamSide(context.getPlayer()).orElse(null),
        context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer ? serverPlayer : null);

    BlockBattlesMod.GAME_LOGIC.onAnyBlockPlaced(serverLevel, pos);

    if (handled) {
      BlockBattlesMod.GAME_LOGIC.syncBattleHands(serverLevel.getServer());
    }

    BlockBattlesMod.GAME_LOGIC.syncTrackedBattleBlocks(serverLevel.getServer());
    BattleScoreboards.updateScoreboard(
        serverLevel.getServer(),
        BlockBattlesMod.BATTLE_STATE);
  }

  private static BlockPos blockBattles$resolvePlacedPos(BlockPlaceContext context, BlockState state) {
    BlockPos clickedPos = context.getClickedPos();

    if (context.getLevel().getBlockState(clickedPos).is(state.getBlock())) {
      return clickedPos;
    }

    BlockPos adjacentPos = clickedPos.relative(context.getClickedFace());

    if (context.getLevel().getBlockState(adjacentPos).is(state.getBlock())) {
      return adjacentPos;
    }

    return clickedPos;
  }

  private static BlockPos blockBattles$resolveSupportPos(BlockPlaceContext context) {
    return blockBattles$resolveSupportPos(
        context.getClickedPos(),
        context.getClickedFace(),
        context.replacingClickedOnBlock());
  }

  private static BlockPos blockBattles$resolveSupportPos(
      BlockPos clickedPos,
      net.minecraft.core.Direction clickedFace,
      boolean replacingClickedOnBlock) {
    if (replacingClickedOnBlock) {
      return clickedPos;
    }

    return clickedPos.relative(clickedFace.getOpposite());
  }
}
