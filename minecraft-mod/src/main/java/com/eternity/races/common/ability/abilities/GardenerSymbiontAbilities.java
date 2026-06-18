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

/** Раса 11 — Садовник-Симбиот: все 8 способностей */
public class GardenerSymbiontAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new GrowthBlessing());
        reg.accept(new HealingJuice());
        reg.accept(new RootBond());
        reg.accept(new NatureArmor());
        reg.accept(new Photosynthesis());
        reg.accept(new Metabolism());
        reg.accept(new FlowerRain());
        reg.accept(new Entangle());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 11.1 Благословение роста — ускорение роста растений в 5 блоках (600t, 2 голода + 2 костяной муки) */
    public static class GrowthBlessing extends AbstractAbility {
        public GrowthBlessing() {
            super(id("growth_blessing"), "ability.racecraft.growth_blessing.name",
                    "ability.racecraft.growth_blessing.desc", 600, 0, 11, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            if (!hasItem(player, Items.BONE_MEAL, 2)) {
                notifyActivation(player, "§2Нужно 2 костяной муки!"); return;
            }
            removeItem(player, Items.BONE_MEAL, 2);
            int radius = 5 + getAccessoryLevel(player) / 2;
            int grown = 0;
            for (BlockPos pos : BlockPos.betweenClosed(
                    player.blockPosition().offset(-radius, -2, -radius),
                    player.blockPosition().offset(radius, 2, radius))) {
                var state = level.getBlockState(pos);
                if (state.is(net.minecraft.tags.BlockTags.CROPS) || state.is(Blocks.WHEAT)
                        || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)
                        || state.is(Blocks.BEETROOTS) || state.is(Blocks.MELON_STEM)
                        || state.is(Blocks.PUMPKIN_STEM)) {
                    net.minecraft.world.level.block.BonemealableBlock bonemeal =
                            state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bb ? bb : null;
                    if (bonemeal != null && bonemeal.isValidBonemealTarget(level, pos, state)) {
                        bonemeal.performBonemeal((net.minecraft.server.level.ServerLevel) level, level.random, pos, state);
                        grown++;
                    }
                }
            }
            notifyActivation(player, "§2Благословение роста! Ускорено " + grown + " растений.");
        }
    }

    /** 11.2 Целебный сок — выпить, восстановить 6♥ (1200t, 1 голод) */
    public static class HealingJuice extends AbstractAbility {
        public HealingJuice() {
            super(id("healing_juice"), "ability.racecraft.healing_juice.name",
                    "ability.racecraft.healing_juice.desc", 1200, 0, 11, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float heal = (6f + getAccessoryLevel(player)) * getDurationMultiplier(player);
            player.heal(heal);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0));
            notifyActivation(player, "§2Целебный сок! +" + (int) heal + "♥ + реген.");
        }
    }

    /** 11.3 Корневая связь — привязка врага на 10 сек (200t, 1 голод) */
    public static class RootBond extends AbstractAbility {
        public RootBond() {
            super(id("root_bond"), "ability.racecraft.root_bond.name",
                    "ability.racecraft.root_bond.desc", 200, 0, 11, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(8);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет врагов в 8 блоках!"); return; }
            int dur = (int)(200 * getDurationMultiplier(player));
            targets.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 127));
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, -10));
                    });
            notifyActivation(player, "§2Корневая связь! Враг обездвижен на 10 сек.");
        }
    }

    /** 11.4 Броня природы — листья снижают урон 15 сек (500t, 1 голод) */
    public static class NatureArmor extends AbstractAbility {
        public NatureArmor() {
            super(id("nature_armor"), "ability.racecraft.nature_armor.name",
                    "ability.racecraft.nature_armor.desc", 500, 20, 11, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.getPersistentData().putLong("nature_armor_expire", level.getGameTime() + dur);
            notifyActivation(player, "§2Броня природы! Защита + шипы 15 сек.");
        }
    }

    /** 11.5 Фотосинтез — пассив: реген HP на солнце (0t, пассив) */
    public static class Photosynthesis extends AbstractAbility {
        public Photosynthesis() {
            super(id("photosynthesis"), "ability.racecraft.photosynthesis.name",
                    "ability.racecraft.photosynthesis.desc", 0, 20, 11, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            notifyActivation(player, "§2Фотосинтез: пассивный реген HP на солнце активен.");
        }
    }

    /** 11.6 Метаболизм — поедание мяса +6♥ (40t, 1 мясо) */
    public static class Metabolism extends AbstractAbility {
        public Metabolism() {
            super(id("gardener_metabolism"), "ability.racecraft.gardener_metabolism.name",
                    "ability.racecraft.gardener_metabolism.desc", 40, 20, 11, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Ищем любое мясо
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_CHICKEN)
                        || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_MUTTON)
                        || stack.is(Items.COOKED_SALMON) || stack.is(Items.BEEF)
                        || stack.is(Items.CHICKEN) || stack.is(Items.PORKCHOP)) {
                    stack.shrink(1);
                    float heal = (6f + getAccessoryLevel(player) * 0.5f) * getDurationMultiplier(player);
                    player.heal(heal);
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0));
                    notifyActivation(player, "§2Метаболизм! +" + (int) heal + "♥ из мяса.");
                    return;
                }
            }
            notifyActivation(player, "§cНет мяса в инвентаре!");
        }
    }

    /** 11.7 Цветочный дождь — 5 цветков → исцеление союзников (2400t, 2 голода + 5 цветков) */
    public static class FlowerRain extends AbstractAbility {
        public FlowerRain() {
            super(id("flower_rain"), "ability.racecraft.flower_rain.name",
                    "ability.racecraft.flower_rain.desc", 2400, 20, 11, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            // Нужно 5 любых цветков
            int flowers = 0;
            for (int i = 0; i < 36 && flowers < 5; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(Items.DANDELION) || s.is(Items.POPPY) || s.is(Items.BLUE_ORCHID)
                        || s.is(Items.ALLIUM) || s.is(Items.OXEYE_DAISY) || s.is(Items.CORNFLOWER)
                        || s.is(Items.SUNFLOWER) || s.is(Items.AZURE_BLUET)) {
                    int take = Math.min(5 - flowers, s.getCount());
                    s.shrink(take);
                    flowers += take;
                }
            }
            if (flowers < 5) { notifyActivation(player, "§cНужно 5 цветков!"); return; }
            float heal = (8f + getAccessoryLevel(player)) * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(15);
            level.getEntitiesOfClass(Player.class, box)
                    .forEach(p -> p.heal(heal));
            notifyActivation(player, "§2Цветочный дождь! Все игроки в 15 блоках +" + (int) heal + "♥.");
        }
    }

    /** 11.8 Опутывание — корни 5 блоков, АоЕ (600t, 1 голод) */
    public static class Entangle extends AbstractAbility {
        public Entangle() {
            super(id("entangle"), "ability.racecraft.entangle.name",
                    "ability.racecraft.entangle.desc", 600, 30, 11, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 5f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(200 * getDurationMultiplier(player));
            float dmg = 3f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 127));
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, -10));
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    });
            notifyActivation(player, "§2Опутывание! Враги скованы в " + (int) radius + " блоках.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
