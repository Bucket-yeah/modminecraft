package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.item.ModItems;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Пакет клиент → сервер. Игрок выбирает расу.
 */
public record SelectRacePacket(int raceId) implements CustomPacketPayload {

    public static final Type<SelectRacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "select_race"));

    public static final StreamCodec<FriendlyByteBuf, SelectRacePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeInt(pkt.raceId),
                    buf -> new SelectRacePacket(buf.readInt())
            );

    public static void handle(SelectRacePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            RaceData data = player.getData(ModAttachments.RACE_DATA.get());

            if (data.hasChosen()) {
                player.sendSystemMessage(Component.literal("§cТы уже выбрал расу! Твой путь предопределён."));
                return;
            }
            if (pkt.raceId < 1 || pkt.raceId > 15) {
                player.sendSystemMessage(Component.literal("§cНеверный ID расы!"));
                return;
            }

            data.selectRace(pkt.raceId);

            // Выдаём кольцо расы
            ItemStack ring = ModItems.getRingForRace(pkt.raceId);
            if (!ring.isEmpty()) {
                player.addItem(ring);
            }

            // Убираем книгу выбора из инвентаря
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == ModItems.RACE_SELECTION_BOOK.get()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    break;
                }
            }

            player.sendSystemMessage(Component.literal(
                    "§6⚔ Ты выбрал расу: §b" + getRaceName(pkt.raceId) + "§6! Твой путь определён."));

            // Синхронизируем с клиентом
            PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
        });
    }

    public static String getRaceName(int id) {
        return switch (id) {
            case 1 -> "Эхо-Голем";
            case 2 -> "Мнемо-Инженер";
            case 3 -> "Химера-Симбионт";
            case 4 -> "Ткач Измерений";
            case 5 -> "Кинетик-Пиромант";
            case 6 -> "Лунный Жнец";
            case 7 -> "Грав-Ткач";
            case 8 -> "Энтропийный Разрушитель";
            case 9 -> "Теневой Дипломат";
            case 10 -> "Алхимик-Трансмутатор";
            case 11 -> "Садовник-Симбиот";
            case 12 -> "Странник-Картограф";
            case 13 -> "Редстоун-Инженер";
            case 14 -> "Меркантильный Маг";
            case 15 -> "Глубинный Гном";
            default -> "Неизвестная раса";
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
