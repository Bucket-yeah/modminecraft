package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.config.RacesConfig;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * Пакет клиент → сервер. Сброс прогрессии (возврат очков).
 */
public record ResetPointsPacket() implements CustomPacketPayload {

    public static final Type<ResetPointsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, "reset_points"));

    public static final StreamCodec<FriendlyByteBuf, ResetPointsPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> {}, buf -> new ResetPointsPacket());

    public static void handle(ResetPointsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!RacesConfig.COMMON.allowReset.get()) {
                player.sendSystemMessage(Component.literal("§cСброс очков отключён на сервере."));
                return;
            }

            RaceData data = player.getData(ModAttachments.RACE_DATA.get());
            if (!data.hasChosen()) return;

            // Проверяем наличие ресурса для сброса
            if (RacesConfig.COMMON.resetCostEnabled.get()) {
                Map.Entry<Item, Integer> cost = getResetCost(data.getRaceId());
                if (cost != null) {
                    int needed = cost.getValue();
                    int found = player.getInventory().countItem(cost.getKey());
                    if (found < needed) {
                        player.sendSystemMessage(Component.literal(
                                "§cДля сброса нужно: §e" + needed + "x " + cost.getKey().getDescription().getString()));
                        return;
                    }
                    // Забираем ресурс
                    int toRemove = needed;
                    for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (stack.getItem() == cost.getKey()) {
                            int remove = Math.min(toRemove, stack.getCount());
                            stack.shrink(remove);
                            toRemove -= remove;
                        }
                    }
                }
            }

            data.resetPoints();
            player.sendSystemMessage(Component.literal("§aОчки успешно сброшены!"));
            PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
        });
    }

    /**
     * Возвращает пару (предмет, количество) для сброса по ID расы.
     */
    private static Map.Entry<Item, Integer> getResetCost(int raceId) {
        net.minecraft.world.item.Items items = null;
        return switch (raceId) {
            case 1  -> Map.entry(net.minecraft.world.item.Items.ECHO_SHARD, 16);
            case 2  -> Map.entry(net.minecraft.world.item.Items.PAPER, 16); // Memory Fragment ~ Paper
            case 3  -> Map.entry(net.minecraft.world.item.Items.STRING, 20);
            case 4  -> Map.entry(net.minecraft.world.item.Items.ENDER_PEARL, 8);
            case 5  -> Map.entry(net.minecraft.world.item.Items.BLAZE_POWDER, 20);
            case 6  -> Map.entry(net.minecraft.world.item.Items.PHANTOM_MEMBRANE, 12);
            case 7  -> Map.entry(net.minecraft.world.item.Items.SLIME_BALL, 32);
            case 8  -> Map.entry(net.minecraft.world.item.Items.OBSIDIAN, 16);
            case 9  -> Map.entry(net.minecraft.world.item.Items.INK_SAC, 24);
            case 10 -> Map.entry(net.minecraft.world.item.Items.GOLD_INGOT, 16);
            case 11 -> Map.entry(net.minecraft.world.item.Items.BONE_MEAL, 32);
            case 12 -> Map.entry(net.minecraft.world.item.Items.COMPASS, 4);
            case 13 -> Map.entry(net.minecraft.world.item.Items.REDSTONE, 32);
            case 14 -> Map.entry(net.minecraft.world.item.Items.EMERALD, 8);
            case 15 -> Map.entry(net.minecraft.world.item.Items.DEEPSLATE, 32);
            default -> null;
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
