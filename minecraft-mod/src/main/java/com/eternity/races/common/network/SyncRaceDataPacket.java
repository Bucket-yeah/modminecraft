package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Пакет сервер → клиент. Синхронизирует данные расы игрока.
 */
public record SyncRaceDataPacket(
        int raceId,
        boolean hasChosen,
        int racePoints,
        boolean[] unlockedAbilities,
        int[] spentPoints,
        int selectedBranch,
        float accessoryExp,
        int accessoryLevel,
        long[] abilityCooldowns
) implements CustomPacketPayload {

    public static final Type<SyncRaceDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "sync_race_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncRaceDataPacket> STREAM_CODEC =
            StreamCodec.of(SyncRaceDataPacket::encode, SyncRaceDataPacket::decode);

    private static void encode(FriendlyByteBuf buf, SyncRaceDataPacket pkt) {
        buf.writeInt(pkt.raceId);
        buf.writeBoolean(pkt.hasChosen);
        buf.writeInt(pkt.racePoints);
        for (boolean b : pkt.unlockedAbilities) buf.writeBoolean(b);
        for (int i : pkt.spentPoints) buf.writeInt(i);
        buf.writeInt(pkt.selectedBranch);
        buf.writeFloat(pkt.accessoryExp);
        buf.writeInt(pkt.accessoryLevel);
        for (long cd : pkt.abilityCooldowns) buf.writeLong(cd);
    }

    private static SyncRaceDataPacket decode(FriendlyByteBuf buf) {
        int raceId = buf.readInt();
        boolean hasChosen = buf.readBoolean();
        int racePoints = buf.readInt();
        boolean[] unlocked = new boolean[8];
        for (int i = 0; i < 8; i++) unlocked[i] = buf.readBoolean();
        int[] spent = new int[8];
        for (int i = 0; i < 8; i++) spent[i] = buf.readInt();
        int branch = buf.readInt();
        float exp = buf.readFloat();
        int level = buf.readInt();
        long[] cooldowns = new long[8];
        for (int i = 0; i < 8; i++) cooldowns[i] = buf.readLong();
        return new SyncRaceDataPacket(raceId, hasChosen, racePoints, unlocked, spent, branch, exp, level, cooldowns);
    }

    /** Создаёт пакет из объекта RaceData */
    public static SyncRaceDataPacket from(RaceData data) {
        return new SyncRaceDataPacket(
                data.getRaceId(),
                data.hasChosen(),
                data.getRacePoints(),
                data.getUnlockedAbilities().clone(),
                data.getSpentPoints().clone(),
                data.getSelectedBranch(),
                data.getAccessoryExp(),
                data.getAccessoryLevel(),
                data.getAbilityCooldowns().clone()
        );
    }

    /** Обработчик на стороне клиента */
    public static void handle(SyncRaceDataPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            RaceData data = player.getData(ModAttachments.RACE_DATA.get());
            applyToData(pkt, data);
        });
    }

    private static void applyToData(SyncRaceDataPacket pkt, RaceData data) {
        // Применяем все поля пакета к локальной копии данных
        if (pkt.hasChosen && !data.hasChosen()) {
            data.selectRace(pkt.raceId);
        }
        data.setRacePoints(pkt.racePoints);
        for (int i = 0; i < 8; i++) {
            if (pkt.unlockedAbilities[i] && !data.isAbilityUnlocked(i)) {
                data.getUnlockedAbilities()[i] = true;
                data.getSpentPoints()[i] = pkt.spentPoints[i];
            }
        }
        data.setSelectedBranch(pkt.selectedBranch);
        data.setAbilityCooldowns(pkt.abilityCooldowns.clone());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
