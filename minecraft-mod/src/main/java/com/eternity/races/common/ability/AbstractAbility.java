package com.eternity.races.common.ability;

import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.config.RacesConfig;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Базовый класс для всех 120 способностей (8 × 15 рас).
 * Подклассы реализуют {@link #execute(Player, Level)}.
 */
public abstract class AbstractAbility {

    /** Уникальный идентификатор способности */
    public final ResourceLocation id;

    /** Ключ локализации для GUI */
    public final String nameKey;

    /** Ключ описания для GUI */
    public final String descKey;

    /** Базовый кулдаун в тиках (без учёта уровня кольца) */
    public final int baseCooldownTicks;

    /** Стоимость разблокировки в очках рас */
    public final int unlockCost;

    /** ID расы, которой принадлежит способность (1-15) */
    public final int raceId;

    /** Индекс в массиве способностей (0-7) */
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

    // ─── Абстрактные методы ──────────────────────────────────────────────────

    /**
     * Основная логика способности. Вызывается на сервере.
     */
    public abstract void execute(Player player, Level level);

    // ─── Конкретные методы ───────────────────────────────────────────────────

    /**
     * Проверяет, может ли игрок использовать способность прямо сейчас.
     */
    public boolean canUse(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (!data.hasChosen() || data.getRaceId() != raceId) return false;
        if (!data.isAbilityUnlocked(abilityIndex)) return false;
        if (!data.isAbilityReady(abilityIndex)) return false;
        return true;
    }

    /**
     * Возвращает эффективный кулдаун в тиках с учётом уровня кольца и конфига.
     * Формула: base * (1 - level * 0.02) * configMultiplier
     */
    public int getCooldownTicks(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        double reduction = 1.0 - (level * 0.02);
        double configMultiplier = RacesConfig.COMMON.cooldownMultiplier.get();
        return (int) Math.max(5, baseCooldownTicks * reduction * configMultiplier);
    }

    /**
     * Возвращает множитель урона с учётом уровня кольца.
     * Формула: 1 + level * 0.05
     */
    public float getDamageMultiplier(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        float configMult = RacesConfig.COMMON.damageMultiplier.get().floatValue();
        return (1f + level * 0.05f) * configMult;
    }

    /**
     * Возвращает множитель длительности с учётом уровня кольца.
     * Формула: 1 + level * 0.03
     */
    public float getDurationMultiplier(Player player) {
        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        int level = data.getAccessoryLevel();
        return 1f + level * 0.03f;
    }

    /**
     * Отправляет игроку сообщение в ActionBar об активации способности.
     */
    protected void notifyActivation(Player player, String message) {
        if (RacesConfig.COMMON.showActionbarMessages.get()) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }

    /**
     * Проверяет, ослаблена ли способность (например, Эхо-Голем на шерсти).
     */
    public boolean isWeakened(Player player) {
        return player.getPersistentData().getBoolean(raceId + "_weakened");
    }
}
