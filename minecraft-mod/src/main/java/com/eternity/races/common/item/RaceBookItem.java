package com.eternity.races.common.item;

import com.eternity.races.client.gui.RaceSelectionScreen;
import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Книга «Том рас» — при использовании открывает GUI выбора расы.
 */
public class RaceBookItem extends Item {

    public RaceBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            RaceData data = player.getData(ModAttachments.RACE_DATA.get());
            if (data.hasChosen()) {
                player.sendSystemMessage(Component.literal("§cТы уже выбрал расу! Твой путь предопределён."));
            } else {
                net.minecraft.client.Minecraft.getInstance().setScreen(new RaceSelectionScreen());
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
