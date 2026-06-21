package com.remy.blockbattles.game.logic;

import java.util.List;

import com.remy.blockbattles.game.gui.BattleScoreboards;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class BattleFeedback {
  private BattleFeedback() {
  }

  public static void syncBattlePlayerVitals(MinecraftServer server, BattleState battleState) {
    if (server == null || battleState == null || !battleState.isGameRunning()) {
      return;
    }

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      TeamSide side = BattlePlayerTeams.getTeamSide(player).orElse(null);

      if (side == null) {
        continue;
      }

      syncBattlePlayerVitals(player, battleState.getTeam(side));
    }
  }

  public static void syncBattlePlayerVitals(ServerPlayer player, BattleTeam team) {
    if (player == null || team == null) {
      return;
    }

    float maxPlayerHealth = player.getMaxHealth();
    float targetHealth = 0.0F;

    if (team.getMaxHealth() > 0) {
      targetHealth = (team.getHealth() / (float) team.getMaxHealth()) * maxPlayerHealth;
    }

    if (team.getHealth() > 0) {
      targetHealth = Math.max(1.0F, targetHealth);
    }

    player.setHealth(Math.max(0.0F, Math.min(maxPlayerHealth, targetHealth)));
    player.setAbsorptionAmount(Math.max(0.0F, Math.min(20.0F, team.getShield() / 10.0F)));
  }

  public static void refreshCombatPresentation(MinecraftServer server, BattleState battleState) {
    if (server == null || battleState == null) {
      return;
    }

    syncBattlePlayerVitals(server, battleState);
    BattleScoreboards.updateScoreboard(server, battleState);
  }

  public static void emitStatFeedback(MinecraftServer server, BattleTeam team, BattleFeedbackType type, int amount) {
    if (team == null || type == null || amount <= 0) {
      return;
    }

    BattleCombatLog.logTeamStatChange(server, team, type, amount);
  }

  public static void emitBlockActivation(
      MinecraftServer server,
      BattleTeam sourceTeam,
      ServerLevel level,
      BlockPos pos,
      BattleFeedbackType type,
      int amount) {
    if (level == null || pos == null || type == null || amount <= 0) {
      return;
    }

    BattleCombatLog.logBlockActivation(
        server,
        sourceTeam == null ? null : sourceTeam.getSide(),
        BattleCombatLog.displayNameForBlock(level.getBlockState(pos).getBlock()),
        type,
        amount);
    spawnBlockActivationParticles(level, pos, type, amount);
    level.playSound(
        null,
        pos,
        getFeedbackSound(type),
        SoundSource.BLOCKS,
        Math.min(2.0F, 0.45F + (amount * 0.04F)),
        type.pitch());
  }

  public static MinecraftServer resolveServer(BattleState battleState, MinecraftServer lastKnownServer) {
    if (lastKnownServer != null) {
      return lastKnownServer;
    }

    if (battleState == null) {
      return null;
    }

    for (BattleTeam team : List.of(battleState.getRedTeam(), battleState.getBlueTeam())) {
      for (PlacedBattleBlock placedBlock : team.getPlacedBlocks()) {
        if (placedBlock.level() != null && placedBlock.level().getServer() != null) {
          return placedBlock.level().getServer();
        }
      }
    }

    return null;
  }

  private static void spawnBlockActivationParticles(ServerLevel level, BlockPos pos, BattleFeedbackType type, int amount) {
    int particleCount = Math.min(80, 4 + Math.max(1, amount) * 2);
    DustParticleOptions particle = new DustParticleOptions(type.color(), 1.15F);

    level.sendParticles(
        particle,
        pos.getX() + 0.5D,
        pos.getY() + 1.05D,
        pos.getZ() + 0.5D,
        particleCount,
        0.18D,
        0.22D,
        0.18D,
        0.01D);
  }

  private static SoundEvent getFeedbackSound(BattleFeedbackType type) {
    return switch (type) {
      case DAMAGE -> SoundEvents.PLAYER_HURT;
      case HEALING -> SoundEvents.EXPERIENCE_ORB_PICKUP;
      case SHIELD_GAIN, SHIELD_LOSS -> SoundEvents.SHIELD_BLOCK.value();
      case MAX_HEALTH -> SoundEvents.BEACON_POWER_SELECT;
    };
  }
}
