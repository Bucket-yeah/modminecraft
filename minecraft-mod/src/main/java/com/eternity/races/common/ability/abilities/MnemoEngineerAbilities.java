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
import java.util.Collections;
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

    /** 2.1 Сохранение точки — до 3 слотов памяти (40t, 1 xp при перезаписи) */
    public static class SavePoint extends AbstractAbility {
        public SavePoint() {
            super(id("save_point"), "ability.racecraft.save_point.name",
                    "ability.racecraft.save_point.desc", 40, 0, 2, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            CompoundTag data = player.getPersistentData();
            for (int i = 0; i < 3; i++) {
                if (!data.contains("mnemo_save_" + i + "_x")) {
                    data.putDouble("mnemo_save_" + i + "_x", player.getX());
                    data.putDouble("mnemo_save_" + i + "_y", player.getY());
                    data.putDouble("mnemo_save_" + i + "_z", player.getZ());
                    data.putFloat("mnemo_save_" + i + "_hp", player.getHealth());
                    data.putInt("mnemo_save_" + i + "_food", player.getFoodData().getFoodLevel());
                    notifyActivation(player, "§dТочка сохранена в слот " + (i + 1) + "!");
                    return;
                }
            }
            // Все слоты заняты — перезаписываем первый, тратим 1 XP
            if (!consumeXp(player, 1)) return;
            data.putDouble("mnemo_save_0_x", player.getX());
            data.putDouble("mnemo_save_0_y", player.getY());
            data.putDouble("mnemo_save_0_z", player.getZ());
            data.putFloat("mnemo_save_0_hp", player.getHealth());
            data.putInt("mnemo_save_0_food", player.getFoodData().getFoodLevel());
            notifyActivation(player, "§dСлот 1 перезаписан (-1 XP)!");
        }
    }

    /** 2.2 Откат — телепорт к сохранённой точке (1200t, 1 xp) */
    public static class Rewind extends AbstractAbility {
        public Rewind() {
            super(id("rewind"), "ability.racecraft.rewind.name",
                    "ability.racecraft.rewind.desc", 1200, 0, 2, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 1)) return;
            CompoundTag data = player.getPersistentData();
            for (int i = 2; i >= 0; i--) {
                if (data.contains("mnemo_save_" + i + "_x")) {
                    double x = data.getDouble("mnemo_save_" + i + "_x");
                    double y = data.getDouble("mnemo_save_" + i + "_y");
                    double z = data.getDouble("mnemo_save_" + i + "_z");
                    float hp = data.getFloat("mnemo_save_" + i + "_hp");
                    int food = data.getInt("mnemo_save_" + i + "_food");
                    player.teleportTo(x, y, z);
                    player.setHealth(Math.min(hp, player.getMaxHealth()));
                    data.remove("mnemo_save_" + i + "_x");
                    data.remove("mnemo_save_" + i + "_y");
                    data.remove("mnemo_save_" + i + "_z");
                    data.remove("mnemo_save_" + i + "_hp");
                    data.remove("mnemo_save_" + i + "_food");
                    notifyActivation(player, "§dОткат к точке " + (i + 1) + "!");
                    return;
                }
            }
            notifyActivation(player, "§cНет сохранённых точек!");
        }
    }

    /** 2.3 Забывание — дезориентация цели 5 сек (600t, золотое яблоко) */
    public static class Oblivion extends AbstractAbility {
        public Oblivion() {
            super(id("oblivion"), "ability.racecraft.oblivion.name",
                    "ability.racecraft.oblivion.desc", 600, 0, 2, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.GOLDEN_APPLE, 1)) {
                notifyActivation(player, "§cНужно золотое яблоко!");
                return;
            }
            removeItem(player, Items.GOLDEN_APPLE, 1);
            int dur = (int)(100 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(8);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей!"); return; }
            LivingEntity target = targets.get(0);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 1));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
            if (target instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(null);
            notifyActivation(player, "§dЗабывание — цель дезориентирована!");
        }
    }

    /** 2.4 Реверс блоков — откат блока (200t, 2 голода) */
    public static class BlockReverse extends AbstractAbility {
        public BlockReverse() {
            super(id("block_reverse"), "ability.racecraft.block_reverse.name",
                    "ability.racecraft.block_reverse.desc", 200, 0, 2, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            var pos = player.blockPosition().relative(player.getDirection());
            var state = level.getBlockState(pos);
            if (!state.isAir()) {
                level.destroyBlock(pos, true, player);
                notifyActivation(player, "§dРеверс блока!");
            } else {
                notifyActivation(player, "§cПрицелься на блок!");
            }
        }
    }

    /** 2.5 Имитация — невидимость 15 сек, мобы не атакуют (800t, 1 голод) */
    public static class Imitation extends AbstractAbility {
        public Imitation() {
            super(id("imitation"), "ability.racecraft.imitation.name",
                    "ability.racecraft.imitation.desc", 800, 20, 2, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            player.getPersistentData().putLong("imitation_expire", level.getGameTime() + dur);
            notifyActivation(player, "§dИмитация — невидимость на 15 сек!");
        }
    }

    /** 2.6 Временная петля — зона возврата 12 сек (1800t, 3 голода) */
    public static class TimeLoop extends AbstractAbility {
        public TimeLoop() {
            super(id("time_loop"), "ability.racecraft.time_loop.name",
                    "ability.racecraft.time_loop.desc", 1800, 20, 2, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            int dur = (int)(240 * getDurationMultiplier(player));
            player.getPersistentData().putLong("time_loop_expire", level.getGameTime() + dur);
            player.getPersistentData().putDouble("time_loop_x", player.getX());
            player.getPersistentData().putDouble("time_loop_y", player.getY());
            player.getPersistentData().putDouble("time_loop_z", player.getZ());
            // Откидываем мобов в зоне назад (симуляция)
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.push(-e.getDeltaMovement().x * 2, 0.3, -e.getDeltaMovement().z * 2);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 1));
                    });
            notifyActivation(player, "§dВременная петля на 12 сек!");
        }
    }

    /** 2.7 Сброс инвентаря — случайные блоки вместо инвентаря (6000t, ничего) */
    public static class InventoryReset extends AbstractAbility {
        private static final ItemStack[] RANDOM_ITEMS = {
            new ItemStack(net.minecraft.world.item.Items.DIRT, 64),
            new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64),
            new ItemStack(net.minecraft.world.item.Items.OAK_LOG, 64),
            new ItemStack(net.minecraft.world.item.Items.SAND, 64),
            new ItemStack(net.minecraft.world.item.Items.GRAVEL, 64)
        };
        public InventoryReset() {
            super(id("inventory_reset"), "ability.racecraft.inventory_reset.name",
                    "ability.racecraft.inventory_reset.desc", 6000, 20, 2, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            // Выбрасываем весь инвентарь
            for (int i = 0; i < 36; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty()) {
                    level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            level, player.getX(), player.getY(), player.getZ(), s.copy()));
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
            // Выдаём 9 случайных стаков
            int count = 9 + getAccessoryLevel(player) / 2;
            for (int i = 0; i < Math.min(count, 15); i++) {
                player.addItem(RANDOM_ITEMS[level.random.nextInt(RANDOM_ITEMS.length)].copy());
            }
            notifyActivation(player, "§dСброс инвентаря! Получены случайные блоки.");
        }
    }

    /** 2.8 Когнитивная перегрузка — ×2 следующая способность, -4♥ (1200t, 4 здоровья) */
    public static class CognitiveOverload extends AbstractAbility {
        public CognitiveOverload() {
            super(id("cognitive_overload"), "ability.racecraft.cognitive_overload.name",
                    "ability.racecraft.cognitive_overload.desc", 1200, 30, 2, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (player.getHealth() <= 4f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
            player.hurt(player.damageSources().magic(), 4f);
            int mult = 2 + getAccessoryLevel(player) / 10;
            player.getPersistentData().putBoolean("cognitive_overload_active", true);
            player.getPersistentData().putLong("cognitive_overload_expire", level.getGameTime() + 100);
            player.getPersistentData().putInt("cognitive_overload_mult", mult);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, mult));
            notifyActivation(player, "§dКогнитивная перегрузка! -4♥, следующая способность ×" + mult + "!");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
