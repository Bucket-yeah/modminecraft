package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbilityManager;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Пакет клиент → сервер. Активация способности по индексу (0-7).
 */
public record AbilityActivationPacket(int abilityIndex) implements CustomPacketPayload {

    public static final Type<AbilityActivationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "ability_activation"));

    public static final StreamCodec<FriendlyByteBuf, AbilityActivationPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeByte(pkt.abilityIndex),
                    buf -> new AbilityActivationPacket(buf.readByte())
            );

    public static void handle(AbilityActivationPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            RaceData data = player.getData(ModAttachments.RACE_DATA.get());

            if (!data.hasChosen()) return;
            if (pkt.abilityIndex < 0 || pkt.abilityIndex > 7) return;
            if (!data.isAbilityUnlocked(pkt.abilityIndex)) return;
            if (!data.isAbilityReady(pkt.abilityIndex)) return;

            AbstractAbility ability = AbilityManager.getAbility(data.getRaceId(), pkt.abilityIndex);
            if (ability == null) return;
            if (!ability.canUse(player)) return;

            ability.execute(player, player.level());

            // Устанавливаем кулдаун с учётом уровня кольца
            long cooldownMs = (long) (ability.getCooldownTicks(player) * 50L);
            data.setAbilityCooldown(pkt.abilityIndex, cooldownMs);

            // Добавляем опыт кольцу
            data.addRingExp(1f);

            // Синхронизируем с клиентом
            PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
