package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 7 — Гравитационный Ткач: все 8 способностей */
public class GravityWeaverAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Inversion());
        reg.accept(new Attraction());
        reg.accept(new Repulsion());
        reg.accept(new Heaviness());
        reg.accept(new Levitation());
        reg.accept(new GravitySphere());
        reg.accept(new Meteorite());
        reg.accept(new GravityWell());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 7.1 Инверсия гравитации — враги парят 3 сек (400t, 1♥) */
    public static class Inversion extends AbstractAbility {
        public Inversion() {
            super(id("inversion"), "ability.racecraft.inversion.name",
                    "ability.racecraft.inversion.desc", 400, 0, 7, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 1f)) return;
            float radius = 8f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(60 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, dur, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 1));
                    });
            notifyActivation(player, "§bИнверсия! Враги парят 3 сек. -1♥");
        }
    }

    /** 7.2 Притяжение — стягивает врагов в центр (300t, 1♥) */
    public static class Attraction extends AbstractAbility {
        public Attraction() {
            super(id("attraction"), "ability.racecraft.attraction.name",
                    "ability.racecraft.attraction.desc", 300, 0, 7, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 1f)) return;
            float radius = 12f * (1f + getAccessoryLevel(player) * 0.05f);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        Vec3 dir = player.position().subtract(e.position()).normalize();
                        e.push(dir.x * 2, dir.y * 0.5 + 0.3, dir.z * 2);
                    });
            notifyActivation(player, "§bПритяжение! Враги втянуты к тебе. -1♥");
        }
    }

    /** 7.3 Отталкивание — отбрасывает врагов (400t, 1♥) */
    public static class Repulsion extends AbstractAbility {
        public Repulsion() {
            super(id("repulsion"), "ability.racecraft.repulsion.name",
                    "ability.racecraft.repulsion.desc", 400, 0, 7, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 1f)) return;
            float radius = 10f * (1f + getAccessoryLevel(player) * 0.05f);
            float dmg = 2f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        Vec3 dir = e.position().subtract(player.position()).normalize();
                        e.push(dir.x * 2.5, 0.6, dir.z * 2.5);
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                    });
            notifyActivation(player, "§bОтталкивание! Враги разлетелись. -1♥");
        }
    }

    /** 7.4 Утяжеление — -70% скорость цели 8 сек (500t, 1♥) */
    public static class Heaviness extends AbstractAbility {
        public Heaviness() {
            super(id("heaviness"), "ability.racecraft.heaviness.name",
                    "ability.racecraft.heaviness.desc", 500, 20, 7, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 1f)) return;
            AABB box = player.getBoundingBox().inflate(10);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей!"); return; }
            int dur = (int)(160 * getDurationMultiplier(player));
            targets.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 4));
                        e.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, -10));
                    });
            notifyActivation(player, "§bУтяжеление! Цель замедлена на 8 сек. -1♥");
        }
    }

    /** 7.5 Левитация — игрок парит 8 сек (240t, 0.5♥) */
    public static class Levitation extends AbstractAbility {
        public Levitation() {
            super(id("levitation"), "ability.racecraft.levitation.name",
                    "ability.racecraft.levitation.desc", 240, 20, 7, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (player.getHealth() <= 1f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
            player.hurt(player.damageSources().magic(), 1f);
            int dur = (int)(160 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, dur, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur, 0));
            notifyActivation(player, "§bЛевитация на 8 сек! -0.5♥");
        }
    }

    /** 7.6 Гравитационная сфера — зона притяжения 15 блоков 30 сек (1200t, 2♥) */
    public static class GravitySphere extends AbstractAbility {
        public GravitySphere() {
            super(id("gravity_sphere"), "ability.racecraft.gravity_sphere.name",
                    "ability.racecraft.gravity_sphere.desc", 1200, 20, 7, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 2f)) return;
            int dur = (int)(600 * getDurationMultiplier(player));
            player.getPersistentData().putLong("gravity_sphere_expire", level.getGameTime() + dur);
            player.getPersistentData().putDouble("gravity_sphere_x", player.getX());
            player.getPersistentData().putDouble("gravity_sphere_y", player.getY());
            player.getPersistentData().putDouble("gravity_sphere_z", player.getZ());
            notifyActivation(player, "§bГравитационная сфера! Притягивает врагов 30 сек. -2♥");
        }
    }

    /** 7.7 Метеорит — призыв падающих блоков на врагов (600t, 2♥) */
    public static class Meteorite extends AbstractAbility {
        public Meteorite() {
            super(id("meteorite"), "ability.racecraft.meteorite.name",
                    "ability.racecraft.meteorite.desc", 600, 20, 7, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 2f)) return;
            float radius = 8f * (1f + getAccessoryLevel(player) * 0.05f);
            float dmg = 8f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 4));
                        level.scheduleTick(e.blockPosition(), net.minecraft.world.level.block.Blocks.SAND, 20);
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                    });
            // Падающий песок
            AABB big = player.getBoundingBox().inflate(radius);
            for (int i = 0; i < 5 + getAccessoryLevel(player); i++) {
                double ox = (level.random.nextDouble() - 0.5) * radius * 2;
                double oz = (level.random.nextDouble() - 0.5) * radius * 2;
                net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(
                        player.getX() + ox, player.getY() + 15, player.getZ() + oz);
                var fe = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, bp,
                        net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
                fe.setHurtsEntities(dmg, 40);
            }
            notifyActivation(player, "§bМетеорит! Камни падают на врагов. -2♥");
        }
    }

    /** 7.8 Гравитационный колодец — зона нулевой гравитации 10 сек (900t, 2♥) */
    public static class GravityWell extends AbstractAbility {
        public GravityWell() {
            super(id("gravity_well"), "ability.racecraft.gravity_well.name",
                    "ability.racecraft.gravity_well.desc", 900, 30, 7, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHealth(player, 2f)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            float radius = 15f * (1f + getAccessoryLevel(player) * 0.05f);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, dur, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    });
            notifyActivation(player, "§bГравитационный колодец! Нулевая гравитация 10 сек. -2♥");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
