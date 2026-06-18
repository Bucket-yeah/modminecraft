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

/** Раса 7 — Грав-Ткач: все 8 способностей */
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

    // 0. Переворот — хождение по потолку
    public static class Inversion extends AbstractAbility {
        public Inversion() {
            super(id("inversion"), "ability.racecraft.inversion.name",
                    "ability.racecraft.inversion.desc", 400, 0, 7, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, (int) dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, (int) dur, 0));
            notifyActivation(player, "§2Переворот — гравитация изменена!");
        }
    }

    // 1. Притяжение — притягивание врагов
    public static class Attraction extends AbstractAbility {
        public Attraction() {
            super(id("attraction"), "ability.racecraft.attraction.name",
                    "ability.racecraft.attraction.desc", 200, 0, 7, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 3f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        Vec3 toPlayer = player.position().subtract(e.position()).normalize();
                        e.push(toPlayer.x * 1.5, 0.3, toPlayer.z * 1.5);
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                    });
            notifyActivation(player, "§2Притяжение!");
        }
    }

    // 2. Отталкивание — отбрасывание врагов
    public static class Repulsion extends AbstractAbility {
        public Repulsion() {
            super(id("repulsion"), "ability.racecraft.repulsion.name",
                    "ability.racecraft.repulsion.desc", 200, 0, 7, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        Vec3 away = e.position().subtract(player.position()).normalize();
                        e.push(away.x * 3, 0.6, away.z * 3);
                    });
            notifyActivation(player, "§2Отталкивание!");
        }
    }

    // 3. Утяжеление — зона замедления и запрета прыжков (Ветка A)
    public static class Heaviness extends AbstractAbility {
        public Heaviness() {
            super(id("heaviness"), "ability.racecraft.heaviness.name",
                    "ability.racecraft.heaviness.desc", 400, 20, 7, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 160 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 2)));
            notifyActivation(player, "§2Утяжеление — зона замедления!");
        }
    }

    // 4. Левитация — полёт 5 блоков (Ветка A)
    public static class Levitation extends AbstractAbility {
        public Levitation() {
            super(id("levitation"), "ability.racecraft.levitation.name",
                    "ability.racecraft.levitation.desc", 300, 20, 7, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, (int) dur, 1));
            notifyActivation(player, "§2Левитация!");
        }
    }

    // 5. Гравитационная сфера — зона низкой гравитации (Ветка B)
    public static class GravitySphere extends AbstractAbility {
        public GravitySphere() {
            super(id("gravity_sphere"), "ability.racecraft.gravity_sphere.name",
                    "ability.racecraft.gravity_sphere.desc", 1200, 20, 7, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 600 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(12);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, (int) dur, 0)));
            notifyActivation(player, "§2Гравитационная сфера — низкая гравитация!");
        }
    }

    // 6. Метеорит — падение блока на врага (Ветка B)
    public static class Meteorite extends AbstractAbility {
        public Meteorite() {
            super(id("meteorite"), "ability.racecraft.meteorite.name",
                    "ability.racecraft.meteorite.desc", 600, 20, 7, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            Vec3 look = player.getLookAngle();
            BlockPos target = player.blockPosition().offset(
                    (int)(look.x * 10), 0, (int)(look.z * 10));
            // Создаём падающий блок
            var fallingBlock = net.minecraft.world.entity.item.FallingBlockEntity.fall(
                    level, target.above(15),
                    net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
            fallingBlock.setHurtsEntities(8f * getDamageMultiplier(player), 40);
            notifyActivation(player, "§2Метеорит запущен!");
        }
    }

    // 7. Гравитационный колодец — непрерывное притяжение (Tier 3)
    public static class GravityWell extends AbstractAbility {
        public GravityWell() {
            super(id("gravity_well"), "ability.racecraft.gravity_well.name",
                    "ability.racecraft.gravity_well.desc", 800, 30, 7, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 400 * getDurationMultiplier(player);
            player.getPersistentData().putLong("gravity_well_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§2Гравитационный колодец — мобы притягиваются!");
        }
    }
}
