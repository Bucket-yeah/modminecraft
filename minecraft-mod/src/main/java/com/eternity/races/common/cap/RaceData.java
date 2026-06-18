package com.eternity.races.common.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранит все данные о расе одного игрока.
 * Сохраняется при смерти (copyOnDeath) и сериализуется в NBT через Codec.
 */
public class RaceData {

    // ─── Сохраняемые поля ───────────────────────────────────────────────────

    /** 0 = не выбрана; 1-15 = ID расы */
    private int raceId = 0;

    /** true — выбор уже сделан и заблокирован */
    private boolean hasChosen = false;

    /** Незатраченные очки рас */
    private int racePoints = 0;

    /** Разблокированные способности (индекс 0-7) */
    private boolean[] unlockedAbilities = new boolean[8];

    /** Очки, вложенные в каждую способность (индекс 0-7) */
    private int[] spentPoints = new int[8];

    /**
     * Выбранная ветка Tier 2:
     *  -1 = не выбрана
     *   0 = Ветка A
     *   1 = Ветка B
     */
    private int selectedBranch = -1;

    /** Опыт кольца */
    private float accessoryExp = 0f;

    /** Уровень кольца (0-10) */
    private int accessoryLevel = 0;

    // ─── Не сохраняемые (runtime) поля ──────────────────────────────────────

    /** Кулдауны способностей в тиках (серверное время) */
    private long[] abilityCooldowns = new long[8];

    // ─── Codec ──────────────────────────────────────────────────────────────

    public static final Codec<RaceData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("raceId").forGetter(d -> d.raceId),
                    Codec.BOOL.fieldOf("hasChosen").forGetter(d -> d.hasChosen),
                    Codec.INT.fieldOf("racePoints").forGetter(d -> d.racePoints),
                    Codec.BOOL.listOf().fieldOf("unlockedAbilities").forGetter(d -> {
                        java.util.List<Boolean> list = new java.util.ArrayList<>();
                        for (boolean b : d.unlockedAbilities) list.add(b);
                        return list;
                    }),
                    Codec.INT.listOf().fieldOf("spentPoints").forGetter(d -> {
                        java.util.List<Integer> list = new java.util.ArrayList<>();
                        for (int i : d.spentPoints) list.add(i);
                        return list;
                    }),
                    Codec.INT.fieldOf("selectedBranch").forGetter(d -> d.selectedBranch),
                    Codec.FLOAT.fieldOf("accessoryExp").forGetter(d -> d.accessoryExp),
                    Codec.INT.fieldOf("accessoryLevel").forGetter(d -> d.accessoryLevel)
            ).apply(instance, (raceId, hasChosen, racePoints, unlocked, spent, branch, exp, level) -> {
                RaceData data = new RaceData();
                data.raceId = raceId;
                data.hasChosen = hasChosen;
                data.racePoints = racePoints;
                for (int i = 0; i < Math.min(unlocked.size(), 8); i++)
                    data.unlockedAbilities[i] = unlocked.get(i);
                for (int i = 0; i < Math.min(spent.size(), 8); i++)
                    data.spentPoints[i] = spent.get(i);
                data.selectedBranch = branch;
                data.accessoryExp = exp;
                data.accessoryLevel = level;
                return data;
            }));

    // ─── Конструктор ────────────────────────────────────────────────────────

    public RaceData() {
        Arrays.fill(unlockedAbilities, false);
        Arrays.fill(spentPoints, 0);
        Arrays.fill(abilityCooldowns, 0L);
    }

    // ─── Бизнес-логика ──────────────────────────────────────────────────────

    /**
     * Выбирает расу (однократно). Автоматически разблокирует 3 способности Tier 1.
     */
    public boolean selectRace(int id) {
        if (hasChosen || id < 1 || id > 15) return false;
        this.raceId = id;
        this.hasChosen = true;
        // Tier 1 (индексы 0, 1, 2) открыты бесплатно
        unlockedAbilities[0] = true;
        unlockedAbilities[1] = true;
        unlockedAbilities[2] = true;
        return true;
    }

    /**
     * Добавляет очки рас с учётом конфиг-множителя.
     */
    public void addPoints(int amount) {
        this.racePoints += amount;
    }

    /**
     * Пытается разблокировать способность по индексу (0-7).
     * Возвращает null при успехе или сообщение об ошибке (для чата).
     */
    public String tryUnlockAbility(int abilityIndex, int cost, int tier, int branchRequired) {
        if (abilityIndex < 0 || abilityIndex > 7)
            return "§cНеверный индекс способности!";
        if (unlockedAbilities[abilityIndex])
            return "§aУже изучено!";
        if (racePoints < cost)
            return "§cНедостаточно очков! Нужно: " + cost;
        if (tier == 2 && branchRequired != -1 && selectedBranch != branchRequired)
            return "§cВыбери ветку " + (branchRequired == 0 ? "A" : "B") + " сначала!";

        racePoints -= cost;
        spentPoints[abilityIndex] = cost;
        unlockedAbilities[abilityIndex] = true;
        return null;
    }

    /**
     * Сбрасывает все потраченные очки в незатраченный пул.
     */
    public void resetPoints() {
        for (int i = 0; i < 8; i++) {
            // Освобождаем только способности Tier 2 и Tier 3 (индексы 3-7)
            if (i >= 3 && unlockedAbilities[i]) {
                racePoints += spentPoints[i];
                spentPoints[i] = 0;
                unlockedAbilities[i] = false;
            }
        }
        selectedBranch = -1;
    }

    /** Добавляет опыт кольцу и повышает уровень при необходимости */
    public void addRingExp(float amount) {
        accessoryExp += amount;
        int newLevel = Math.min(10, (int) (accessoryExp / 100f));
        this.accessoryLevel = newLevel;
    }

    /** Устанавливает кулдаун способности в миллисекундах */
    public void setAbilityCooldown(int index, long cooldownMs) {
        if (index >= 0 && index < 8)
            abilityCooldowns[index] = System.currentTimeMillis() + cooldownMs;
    }

    /** Проверяет, прошёл ли кулдаун */
    public boolean isAbilityReady(int index) {
        if (index < 0 || index >= 8) return false;
        return System.currentTimeMillis() >= abilityCooldowns[index];
    }

    /** Возвращает оставшийся кулдаун в тиках (для отображения) */
    public int getRemainingCooldownTicks(int index) {
        if (index < 0 || index >= 8) return 0;
        long remaining = abilityCooldowns[index] - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (int) (remaining / 50);
    }

    // ─── Геттеры/Сеттеры ────────────────────────────────────────────────────

    public int getRaceId() { return raceId; }
    public boolean hasChosen() { return hasChosen; }
    public int getRacePoints() { return racePoints; }
    public void setRacePoints(int racePoints) { this.racePoints = racePoints; }
    public boolean isAbilityUnlocked(int index) { return index >= 0 && index < 8 && unlockedAbilities[index]; }
    public boolean[] getUnlockedAbilities() { return unlockedAbilities; }
    public int[] getSpentPoints() { return spentPoints; }
    public int getSelectedBranch() { return selectedBranch; }
    public void setSelectedBranch(int branch) { this.selectedBranch = branch; }
    public float getAccessoryExp() { return accessoryExp; }
    public int getAccessoryLevel() { return accessoryLevel; }
    public long[] getAbilityCooldowns() { return abilityCooldowns; }
    public void setAbilityCooldowns(long[] cooldowns) { this.abilityCooldowns = cooldowns; }
}
