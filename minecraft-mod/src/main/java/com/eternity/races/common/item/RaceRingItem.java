package com.eternity.races.common.item;

import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.network.SelectRacePacket;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.function.Supplier;

/**
 * Базовый класс для колец рас.
 * Каждая раса имеет собственный экземпляр с уникальным ID расы.
 */
public class RaceRingItem extends Item implements ICurioItem {

    private final int raceId;
    private final String raceName;
    private final String passiveDesc;
    private final String penaltyDesc;

    public RaceRingItem(int raceId, String raceName, String passiveDesc, String penaltyDesc) {
        super(new Item.Properties().stacksTo(1));
        this.raceId = raceId;
        this.raceName = raceName;
        this.passiveDesc = passiveDesc;
        this.penaltyDesc = penaltyDesc;
    }

    // ─── Curios интерфейс ────────────────────────────────────────────────────

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack currentStack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof ServerPlayer player)) return;

        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (!data.hasChosen()) {
            // Игрок ещё не выбрал расу — выбираем её через пакет
            data.selectRace(raceId);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    player,
                    com.eternity.races.common.network.SyncRaceDataPacket.from(data)
            );
        } else if (data.getRaceId() != raceId) {
            // Попытка надеть кольцо другой расы — запрещаем
            player.sendSystemMessage(Component.literal("§cТы не можешь носить кольцо другой расы!"));
            // Убираем кольцо из слота Curios (вернём в инвентарь)
            player.addItem(new ItemStack(this));
            // Физически снять нельзя напрямую в onEquip, поэтому помечаем
            player.getPersistentData().putBoolean("racecraft_reject_ring", true);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack currentStack) {
        // Кольцо расы нельзя снять после выбора расы
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof ServerPlayer player)) return;
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (data.hasChosen() && !newStack.isEmpty()) {
            // Возвращаем кольцо обратно, если снятие не разрешено
            // (логика принудительного возврата обрабатывается в RaceEventHandler)
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack currentStack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player)) return true;
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        // Нельзя надеть кольцо другой расы
        if (data.hasChosen() && data.getRaceId() != raceId) return false;
        return true;
    }

    // ─── Tooltip ─────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Данные расы только с клиентской стороны
        if (context.level() == null) return;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());

        tooltipComponents.add(Component.literal("§6Кольцо: " + raceName));
        tooltipComponents.add(Component.literal("§7Уровень: §e" + data.getAccessoryLevel() + "/10"));
        tooltipComponents.add(Component.literal("§7Опыт: §e" + (int) data.getAccessoryExp()
                + " / " + (data.getAccessoryLevel() * 100 + 100)));
        tooltipComponents.add(Component.literal("§7Пассив: §a" + passiveDesc));
        tooltipComponents.add(Component.literal("§7Штраф: §c" + penaltyDesc));
    }

    public int getRaceId() { return raceId; }
    public String getRaceName() { return raceName; }
}
