package com.eternity.races.common.ability;

import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.config.RacesConfig;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Базовый класс для всех 120 способностей (8 × 15 рас).
 * Подклассы реализуют {@link #execute(Player, Level)}.
 */
public abstract class AbstractAbility {

    public final ResourceLocation id;
    public final String nameKey;
    public final String descKey;
    public final int baseCooldownTicks;
    public final int unlockCost;
    public final int raceId;
    public final int abilityIndex;

    protected AbstractAbility(ResourceLocation id, String nameKey, String descKey,
                               int baseCooldownTicks, int unlockCost, int raceId, int abilityIndex) {
        this.id = id;
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.baseCooldownTicks = baseCooldownTicks;
        this.unlockCost = unlockCost;
        this.raceId = raceId;
        this.abilityIndex = abilityIndex;
    }

    public abstract void execute(Player player, Level level);

    public boolean canUse(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (!data.hasChosen() || data.getRaceId() != raceId) return false;
        if (!data.isAbilityUnlocked(abilityIndex)) return false;
        if (!data.isAbilityReady(abilityIndex)) return false;
        return true;
    }

    public int getCooldownTicks(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        double reduction = 1.0 - (level * 0.02);
        double configMultiplier = RacesConfig.COMMON.cooldownMultiplier.get();
        return (int) Math.max(5, baseCooldownTicks * reduction * configMultiplier);
    }

    public float getDamageMultiplier(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        float configMult = RacesConfig.COMMON.damageMultiplier.get().floatValue();
        return (1f + level * 0.05f) * configMult;
    }

    public float getDurationMultiplier(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        return 1f + level * 0.03f;
    }

    protected void notifyActivation(Player player, String message) {
        if (RacesConfig.COMMON.showActionbarMessages.get()) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }

    public boolean isWeakened(Player player) {
        return player.getPersistentData().getBoolean(raceId + "_weakened");
    }

    // ─── Утилиты для расхода ресурсов ────────────────────────────────────────

    protected boolean consumeHunger(Player player, int halvesOfFood) {
        if (player.getFoodData().getFoodLevel() < halvesOfFood) {
            notifyActivation(player, "§cНедостаточно голода!");
            return false;
        }
        player.getFoodData().addExhaustion(halvesOfFood * 4f);
        return true;
    }

    protected boolean consumeHealth(Player player, float damage) {
        if (player.getHealth() <= damage) {
            notifyActivation(player, "§cНедостаточно здоровья!");
            return false;
        }
        player.hurt(player.damageSources().magic(), damage);
        return true;
    }

    protected boolean consumeXp(Player player, int levels) {
        if (player.experienceLevel < levels) {
            notifyActivation(player, "§cНедостаточно уровней опыта (нужно " + levels + ")!");
            return false;
        }
        player.giveExperienceLevels(-levels);
        return true;
    }

    protected boolean hasItem(Player player, Item item, int count) {
        return player.getInventory().countItem(item) >= count;
    }

    protected void removeItem(Player player, Item item, int count) {
        int toRemove = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(toRemove, stack.getCount());
                stack.shrink(take);
                toRemove -= take;
            }
        }
    }
}
