package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Раса 2 — Мнемо-Инженер: все 8 способностей */
public class MnemoEngineerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new SavePoint());
        reg.accept(new Rewind());
        reg.accept(new Oblivion());
        reg.accept(new BlockReverse());
        reg.accept(new Imitation());
        reg.accept(new TimeLoop());
        reg.accept(new InventoryReset());
        reg.accept(new CognitiveOverload());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Сохранение точки — запоминает до 3 позиций
    public static class SavePoint extends AbstractAbility {
        public SavePoint() {
            super(id("save_point"), "ability.racecraft.save_point.name",
                    "ability.racecraft.save_point.desc", 60, 0, 2, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            CompoundTag data = player.getPersistentData();
            // Находим первый свободный слот
            for (int i = 0; i < 3; i++) {
                if (!data.contains("mnemo_save_" + i + "_x")) {
                    data.putDouble("mnemo_save_" + i + "_x", player.getX());
                    data.putDouble("mnemo_save_" + i + "_y", player.getY());
                    data.putDouble("mnemo_save_" + i + "_z", player.getZ());
                    data.putFloat("mnemo_save_" + i + "_hp", player.getHealth());
                    notifyActivation(player, "§dТочка сохранена в слоте " + (i + 1) + "!");
                    return;
                }
            }
            notifyActivation(player, "§cВсе слоты заняты! Сначала используй Откат.");
        }
    }

    // 1. Откат — телепорт к сохранённой точке
    public static class Rewind extends AbstractAbility {
        public Rewind() {
            super(id("rewind"), "ability.racecraft.rewind.name",
                    "ability.racecraft.rewind.desc", 200, 0, 2, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            CompoundTag data = player.getPersistentData();
            // Используем последнюю сохранённую точку
            for (int i = 2; i >= 0; i--) {
                if (data.contains("mnemo_save_" + i + "_x")) {
                    double x = data.getDouble("mnemo_save_" + i + "_x");
                    double y = data.getDouble("mnemo_save_" + i + "_y");
                    double z = data.getDouble("mnemo_save_" + i + "_z");
                    float hp = data.getFloat("mnemo_save_" + i + "_hp");
                    player.teleportTo(x, y, z);
                    player.setHealth(Math.min(hp, player.getMaxHealth()));
                    // Очищаем слот
                    data.remove("mnemo_save_" + i + "_x");
                    data.remove("mnemo_save_" + i + "_y");
                    data.remove("mnemo_save_" + i + "_z");
                    data.remove("mnemo_save_" + i + "_hp");
                    notifyActivation(player, "§dОткат выполнен!");
                    return;
                }
            }
            notifyActivation(player, "§cНет сохранённых точек!");
        }
    }

    // 2. Забывание — цель дезориентирована 5 сек (стоит золотое яблоко)
    public static class Oblivion extends AbstractAbility {
        public Oblivion() {
            super(id("oblivion"), "ability.racecraft.oblivion.name",
                    "ability.racecraft.oblivion.desc", 400, 0, 2, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            // Проверяем наличие золотого яблока
            boolean hasApple = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(Items.GOLDEN_APPLE)) {
                    player.getInventory().getItem(i).shrink(1);
                    hasApple = true;
                    break;
                }
            }
            if (!hasApple) {
                notifyActivation(player, "§cНужно золотое яблоко!");
                return;
            }
            float dur = 100 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8).inflate(0, 2, 0);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) dur, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) dur, 0));
                    });
            notifyActivation(player, "§dЗабывание применено!");
        }
    }

    // 3. Реверс блоков — откатывает состояние блока
    public static class BlockReverse extends AbstractAbility {
        public BlockReverse() {
            super(id("block_reverse"), "ability.racecraft.block_reverse.name",
                    "ability.racecraft.block_reverse.desc", 300, 20, 2, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            var pos = player.blockPosition().relative(player.getDirection());
            var state = level.getBlockState(pos);
            if (!state.isAir()) {
                // Убираем блок и возвращаем предмет
                level.destroyBlock(pos, true, player);
                notifyActivation(player, "§dРеверс блока!");
            }
        }
    }

    // 4. Имитация — копирование внешности существа (Ветка A)
    public static class Imitation extends AbstractAbility {
        public Imitation() {
            super(id("imitation"), "ability.racecraft.imitation.name",
                    "ability.racecraft.imitation.desc", 600, 20, 2, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 300 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) dur, 0));
            player.getPersistentData().putLong("imitation_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§dИмитация — ты невидим!");
        }
    }

    // 5. Временная петля — зона возврата позиций (Ветка A)
    public static class TimeLoop extends AbstractAbility {
        public TimeLoop() {
            super(id("time_loop"), "ability.racecraft.time_loop.name",
                    "ability.racecraft.time_loop.desc", 800, 20, 2, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Откидываем мобов в зоне назад
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        // Возвращаем в позицию 3 секунды назад (симуляция)
                        e.push(-e.getDeltaMovement().x * 2, 0.3, -e.getDeltaMovement().z * 2);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    });
            notifyActivation(player, "§dВременная петля!");
        }
    }

    // 6. Сброс инвентаря — заменяет инвентарь случайными предметами (Ветка B)
    public static class InventoryReset extends AbstractAbility {
        public InventoryReset() {
            super(id("inventory_reset"), "ability.racecraft.inventory_reset.name",
                    "ability.racecraft.inventory_reset.desc", 1200, 20, 2, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            // Применяем к ближайшему врагу
            AABB box = player.getBoundingBox().inflate(5);
            var targets = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, box);
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(0);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 2));
                notifyActivation(player, "§dСброс инвентаря врага!");
            }
        }
    }

    // 7. Когнитивная перегрузка — удваивает следующую способность, -4♥ (Tier 3)
    public static class CognitiveOverload extends AbstractAbility {
        public CognitiveOverload() {
            super(id("cognitive_overload"), "ability.racecraft.cognitive_overload.name",
                    "ability.racecraft.cognitive_overload.desc", 600, 30, 2, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            player.hurt(player.damageSources().magic(), 4f);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 3));
            player.getPersistentData().putBoolean("cognitive_overload_active", true);
            notifyActivation(player, "§dКогнитивная перегрузка! -4♥, следующая способность ×2!");
        }
    }
}
