package com.remy.blockbattles.network;

import java.util.ArrayList;
import java.util.List;

import com.remy.blockbattles.BlockBattlesMod;
import com.remy.blockbattles.game.logic.TeamSide;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BattleBlockOutlinePayload(List<OutlinedBlock> blocks) implements CustomPacketPayload {
  public static final Type<BattleBlockOutlinePayload> TYPE = new Type<>(
      Identifier.fromNamespaceAndPath(BlockBattlesMod.MOD_ID, "battle_block_outlines"));
  public static final StreamCodec<RegistryFriendlyByteBuf, BattleBlockOutlinePayload> CODEC = StreamCodec.of(
      (buf, payload) -> payload.write(buf),
      BattleBlockOutlinePayload::read);

  public BattleBlockOutlinePayload(List<OutlinedBlock> blocks) {
    this.blocks = List.copyOf(blocks);
  }

  @Override
  public Type<BattleBlockOutlinePayload> type() {
    return TYPE;
  }

  private void write(RegistryFriendlyByteBuf buf) {
    buf.writeVarInt(blocks.size());

    for (OutlinedBlock block : blocks) {
      buf.writeUtf(block.dimensionId());
      buf.writeLong(block.pos().asLong());
      buf.writeUtf(block.ownerSide().name());
    }
  }

  private static BattleBlockOutlinePayload read(RegistryFriendlyByteBuf buf) {
    int blockCount = buf.readVarInt();
    ArrayList<OutlinedBlock> blocks = new ArrayList<>(blockCount);

    for (int index = 0; index < blockCount; index++) {
      blocks.add(new OutlinedBlock(
          buf.readUtf(),
          BlockPos.of(buf.readLong()),
          TeamSide.valueOf(buf.readUtf())));
    }

    return new BattleBlockOutlinePayload(blocks);
  }

  public record OutlinedBlock(String dimensionId, BlockPos pos, TeamSide ownerSide) {
  }
}
