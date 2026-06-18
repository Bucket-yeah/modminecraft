package com.eternity.races.common.ability;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.abilities.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Реестр всех 120 способностей (8 × 15 рас).
 * Ключ: (raceId, abilityIndex) → AbstractAbility
 */
public class AbilityManager {

    private static final Map<Long, AbstractAbility> REGISTRY = new LinkedHashMap<>();

    static {
        registerAll();
    }

    private static void register(AbstractAbility ability) {
        long key = makeKey(ability.raceId, ability.abilityIndex);
        REGISTRY.put(key, ability);
    }

    private static long makeKey(int raceId, int index) {
        return ((long) raceId << 8) | (index & 0xFF);
    }

    /**
     * Возвращает способность по расе и индексу, или null если не найдена.
     */
    public static AbstractAbility getAbility(int raceId, int abilityIndex) {
        return REGISTRY.get(makeKey(raceId, abilityIndex));
    }

    /**
     * Возвращает все 8 способностей расы.
     */
    public static List<AbstractAbility> getAbilitiesForRace(int raceId) {
        List<AbstractAbility> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            AbstractAbility a = getAbility(raceId, i);
            if (a != null) result.add(a);
        }
        return result;
    }

    /**
     * Возвращает уровень (tier) способности по индексу:
     * 0-2 → Tier 1 (бесплатно)
     * 3-6 → Tier 2 (ветки A и B)
     * 7   → Tier 3
     */
    public static int getTierForIndex(int raceId, int index) {
        if (index <= 2) return 1;
        if (index <= 6) return 2;
        return 3;
    }

    /**
     * Возвращает стоимость разблокировки способности.
     */
    public static int getCostForIndex(int raceId, int index) {
        AbstractAbility ability = getAbility(raceId, index);
        return ability != null ? ability.unlockCost : 20;
    }

    /**
     * Возвращает требуемую ветку для Tier 2 способности:
     * индексы 3,4 → ветка A (0)
     * индексы 5,6 → ветка B (1)
     * иначе → -1 (ветка не требуется)
     */
    public static int getBranchForIndex(int raceId, int index) {
        if (index == 3 || index == 4) return 0;
        if (index == 5 || index == 6) return 1;
        return -1;
    }

    // ─── Регистрация всех способностей ──────────────────────────────────────

    private static void registerAll() {
        // Раса 1: Эхо-Голем
        EchoGolemAbilities.registerAll(AbilityManager::register);
        // Раса 2: Мнемо-Инженер
        MnemoEngineerAbilities.registerAll(AbilityManager::register);
        // Раса 3: Химера-Симбионт
        ChimeraSymbiontAbilities.registerAll(AbilityManager::register);
        // Раса 4: Ткач Измерений
        DimensionWeaverAbilities.registerAll(AbilityManager::register);
        // Раса 5: Кинетик-Пиромант
        KineticPyromancerAbilities.registerAll(AbilityManager::register);
        // Раса 6: Лунный Жнец
        LunarReaperAbilities.registerAll(AbilityManager::register);
        // Раса 7: Грав-Ткач
        GravityWeaverAbilities.registerAll(AbilityManager::register);
        // Раса 8: Энтропийный Разрушитель
        EntropyDestroyerAbilities.registerAll(AbilityManager::register);
        // Раса 9: Теневой Дипломат
        ShadowDiplomatAbilities.registerAll(AbilityManager::register);
        // Раса 10: Алхимик-Трансмутатор
        AlchemistTransmutorAbilities.registerAll(AbilityManager::register);
        // Раса 11: Садовник-Симбиот
        GardenerSymbiontAbilities.registerAll(AbilityManager::register);
        // Раса 12: Странник-Картограф
        WandererCartographerAbilities.registerAll(AbilityManager::register);
        // Раса 13: Редстоун-Инженер
        RedstoneEngineerAbilities.registerAll(AbilityManager::register);
        // Раса 14: Меркантильный Маг
        MercantileMageAbilities.registerAll(AbilityManager::register);
        // Раса 15: Глубинный Гном
        DeepGnomeAbilities.registerAll(AbilityManager::register);
    }
}
