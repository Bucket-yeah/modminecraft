package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbilityManager;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Пакет клиент → сервер. Покупка способности в дереве прогрессии.
 */
public record UnlockAbilityPacket(int abilityIndex, int branchChoice) implements CustomPacketPayload {

    public static final Type<UnlockAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "unlock_ability"));

    public static final StreamCodec<FriendlyByteBuf, UnlockAbilityPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> { buf.writeByte(pkt.abilityIndex); buf.writeByte(pkt.branchChoice); },
                    buf -> new UnlockAbilityPacket(buf.readByte(), buf.readByte())
            );

    public static void handle(UnlockAbilityPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            RaceData data = player.getData(ModAttachments.RACE_DATA.get());
            if (!data.hasChosen()) return;

            int tier = AbilityManager.getTierForIndex(data.getRaceId(), pkt.abilityIndex);
            int cost = AbilityManager.getCostForIndex(data.getRaceId(), pkt.abilityIndex);

            // Проверяем пороговые требования
            int totalSpent = 0;
            for (int sp : data.getSpentPoints()) totalSpent += sp;
            if (tier == 2 && totalSpent < 0) {
                player.sendSystemMessage(Component.literal("§cНужно ещё очков для Tier 2!"));
                return;
            }
            if (tier == 3 && totalSpent < 100) {
                player.sendSystemMessage(Component.literal("§cНужно 100 потраченных очков для Tier 3!"));
                return;
            }

            // Устанавливаем ветку, если это Tier 2
            if (tier == 2 && data.getSelectedBranch() == -1 && pkt.branchChoice >= 0) {
                data.setSelectedBranch(pkt.branchChoice);
            }

            int requiredBranch = (tier == 2) ? AbilityManager.getBranchForIndex(data.getRaceId(), pkt.abilityIndex) : -1;
            String error = data.tryUnlockAbility(pkt.abilityIndex, cost, tier, requiredBranch);

            if (error != null) {
                player.sendSystemMessage(Component.literal(error));
            } else {
                player.sendSystemMessage(Component.literal("§aСпособность изучена!"));
                PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
