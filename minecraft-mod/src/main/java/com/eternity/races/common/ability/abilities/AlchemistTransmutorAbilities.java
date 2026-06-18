package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 10 — Алхимик-Трансмутатор: все 8 способностей */
public class AlchemistTransmutorAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Transmutation());
        reg.accept(new Hardening());
        reg.accept(new Liquify());
        reg.accept(new AlchemyShield());
        reg.accept(new Metabolism());
        reg.accept(new GoldenTouch());
        reg.accept(new Stabilization());
        reg.accept(new Synthesis());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 10.1 Трансмутация — блок в другой (100t, 1 голод) */
    public static class Transmutation extends AbstractAbility {
        public Transmutation() {
            super(id("transmutation"), "ability.racecraft.transmutation.name",
                    "ability.racecraft.transmutation.desc", 100, 0, 10, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            BlockPos pos = player.blockPosition().relative(player.getDirection());
            var state = level.getBlockState(pos);
            if (state.isAir() || state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
                notifyActivation(player, "§aПрицелься на обычный блок!");
                return;
            }
            // Таблица трансмутации
            var block = state.getBlock();
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
            else if (block == Blocks.SAND) level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
            else if (block == Blocks.COBBLESTONE) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            else if (block == Blocks.STONE) level.setBlockAndUpdate(pos, Blocks.GOLD_BLOCK.defaultBlockState());
            else if (block == Blocks.GOLD_BLOCK) level.setBlockAndUpdate(pos, Blocks.IRON_BLOCK.defaultBlockState());
            else if (block == Blocks.IRON_BLOCK) level.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
            else if (block == Blocks.GRAVEL) level.setBlockAndUpdate(pos, Blocks.COAL_ORE.defaultBlockState());
            else if (block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG || block == Blocks.SPRUCE_LOG)
                level.setBlockAndUpdate(pos, Blocks.IRON_ORE.defaultBlockState());
            else {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            notifyActivation(player, "§aТрансмутация!");
        }
    }

    /** 10.2 Закалка — +40% защита 15 сек, -20% скорость (600t, 1 голод) */
    public static class Hardening extends AbstractAbility {
        public Hardening() {
            super(id("hardening"), "ability.racecraft.hardening.name",
                    "ability.racecraft.hardening.desc", 600, 0, 10, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
            notifyActivation(player, "§aЗакалка! +40% защита, -20% скорость 15 сек.");
        }
    }

    /** 10.3 Разжижение — жидкий металл 10 сек, +урон (400t, 1 голод) */
    public static class Liquify extends AbstractAbility {
        public Liquify() {
            super(id("liquify"), "ability.racecraft.liquify.name",
                    "ability.racecraft.liquify.desc", 400, 0, 10, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float dmg = 5f * getDamageMultiplier(player);
            int dur = (int)(200 * getDurationMultiplier(player));
            // АоЕ удар жидким металлом
            AABB box = player.getBoundingBox().inflate(3);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 1));
                    });
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1));
            notifyActivation(player, "§aРазжижение! Удар металлом, бонус урона 10 сек.");
        }
    }

    /** 10.4 Алхимический щит — поглощение 10 урона (300t, 1 голод) */
    public static class AlchemyShield extends AbstractAbility {
        public AlchemyShield() {
            super(id("alchemy_shield"), "ability.racecraft.alchemy_shield.name",
                    "ability.racecraft.alchemy_shield.desc", 300, 20, 10, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            int absLevel = 4 + getAccessoryLevel(player) / 2;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, absLevel));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 0));
            notifyActivation(player, "§aАлхимический щит! Поглощение " + (absLevel + 1) * 4 + "♥.");
        }
    }

    /** 10.5 Метаболизм — еда даёт ×2 насыщение (200t, ничего) */
    public static class Metabolism extends AbstractAbility {
        public Metabolism() {
            super(id("metabolism"), "ability.racecraft.metabolism.name",
                    "ability.racecraft.metabolism.desc", 200, 20, 10, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            int dur = (int)(400 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, dur, 1));
            notifyActivation(player, "§aМетаболизм! Реген + насыщение на 20 сек.");
        }
    }

    /** 10.6 Золотое прикосновение — преобразование предметов в золото (1200t, 2 голода) */
    public static class GoldenTouch extends AbstractAbility {
        public GoldenTouch() {
            super(id("golden_touch"), "ability.racecraft.golden_touch.name",
                    "ability.racecraft.golden_touch.desc", 1200, 20, 10, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int converted = 0;
            for (int i = 0; i < 36 && converted < 5 + getAccessoryLevel(player); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && !stack.is(Items.GOLD_INGOT) && !stack.is(Items.GOLD_BLOCK)) {
                    player.getInventory().setItem(i, new ItemStack(Items.GOLD_INGOT, stack.getCount()));
                    converted++;
                }
            }
            notifyActivation(player, "§aЗолотое прикосновение! " + converted + " стаков → золото.");
        }
    }

    /** 10.7 Стабилизация — стабилизация предметов на 2 мин (2400t, 3 XP уровня) */
    public static class Stabilization extends AbstractAbility {
        public Stabilization() {
            super(id("stabilization"), "ability.racecraft.stabilization.name",
                    "ability.racecraft.stabilization.desc", 2400, 20, 10, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 3)) return;
            int dur = (int)(2400 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 0));
            player.getPersistentData().putLong("alchimist_stable_expire", level.getGameTime() + dur);
            notifyActivation(player, "§aСтабилизация на 2 мин! Огнеупорность + защита + реген.");
        }
    }

    /** 10.8 Синтез — 3 предмета → улучшенный (400t, 3 предмета) */
    public static class Synthesis extends AbstractAbility {
        public Synthesis() {
            super(id("synthesis"), "ability.racecraft.synthesis.name",
                    "ability.racecraft.synthesis.desc", 400, 30, 10, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            int found = 0;
            int firstSlot = -1;
            for (int i = 0; i < 36 && found < 3; i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    if (firstSlot == -1) firstSlot = i;
                    player.getInventory().getItem(i).shrink(1);
                    found++;
                }
            }
            if (found < 3) { notifyActivation(player, "§cНужно минимум 3 предмета в инвентаре!"); return; }
            int lvl = getAccessoryLevel(player);
            ItemStack result;
            int r = level.random.nextInt(5 + lvl);
            if (r == 0) result = new ItemStack(Items.DIAMOND, 1 + lvl / 4);
            else if (r == 1) result = new ItemStack(Items.EMERALD, 2 + lvl / 3);
            else if (r == 2) result = new ItemStack(Items.GOLD_INGOT, 3 + lvl / 2);
            else if (r == 3) result = new ItemStack(Items.IRON_INGOT, 5 + lvl);
            else result = new ItemStack(Items.EXPERIENCE_BOTTLE, 3);
            player.addItem(result);
            notifyActivation(player, "§aСинтез! Получен: " + result.getDescriptionId());
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
