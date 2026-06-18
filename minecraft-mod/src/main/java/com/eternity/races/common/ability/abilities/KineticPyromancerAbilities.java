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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 5 — Кинетик-Пиромант: все 8 способностей */
public class KineticPyromancerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Acceleration());
        reg.accept(new Spark());
        reg.accept(new ThermoShield());
        reg.accept(new KineticStrike());
        reg.accept(new HeatAbsorption());
        reg.accept(new ExplosiveRun());
        reg.accept(new Overload());
        reg.accept(new FireTrail());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Разгон — +60% скорость 10 сек, затем -30% 5 сек
    public static class Acceleration extends AbstractAbility {
        public Acceleration() {
            super(id("acceleration"), "ability.racecraft.acceleration.name",
                    "ability.racecraft.acceleration.desc", 300, 0, 5, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) dur, 2));
            player.getPersistentData().putLong("pyro_slow_start", level.getGameTime() + (long) dur);
            notifyActivation(player, "§6Разгон! +60% скорость");
        }
    }

    // 1. Искра — огненный шар
    public static class Spark extends AbstractAbility {
        public Spark() {
            super(id("spark"), "ability.racecraft.spark.name",
                    "ability.racecraft.spark.desc", 200, 0, 5, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            Vec3 look = player.getLookAngle();
            var fireball = new net.minecraft.world.entity.projectile.SmallFireball(level, player,
                    look.x * 1.5, look.y * 1.5, look.z * 1.5);
            fireball.setPos(player.getX() + look.x * 2, player.getY() + 1.5, player.getZ() + look.z * 2);
            level.addFreshEntity(fireball);
            notifyActivation(player, "§6Искра!");
        }
    }

    // 2. Термо-щит — огненный барьер
    public static class ThermoShield extends AbstractAbility {
        public ThermoShield() {
            super(id("thermo_shield"), "ability.racecraft.thermo_shield.name",
                    "ability.racecraft.thermo_shield.desc", 400, 0, 5, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (int) dur + 200, 0));
            player.getPersistentData().putLong("thermo_shield_expire", level.getGameTime() + (long) dur);
            // Поджигаем мобов при касании (обрабатывается в RaceEventHandler)
            notifyActivation(player, "§6Термо-щит активирован!");
        }
    }

    // 3. Кинетический удар — урон зависит от скорости (Ветка A)
    public static class KineticStrike extends AbstractAbility {
        public KineticStrike() {
            super(id("kinetic_strike"), "ability.racecraft.kinetic_strike.name",
                    "ability.racecraft.kinetic_strike.desc", 200, 20, 5, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            double speed = player.getDeltaMovement().horizontalDistance();
            float damage = (float) (speed * 10 * getDamageMultiplier(player));
            damage = Math.max(2f, Math.min(damage, 20f));
            AABB box = player.getBoundingBox().inflate(2).inflate(0, 1, 0);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage));
            notifyActivation(player, "§6Кинетический удар! Урон: " + (int) damage);
        }
    }

    // 4. Поглощение тепла — лечение от огня/лавы (Ветка A)
    public static class HeatAbsorption extends AbstractAbility {
        public HeatAbsorption() {
            super(id("heat_absorption"), "ability.racecraft.heat_absorption.name",
                    "ability.racecraft.heat_absorption.desc", 300, 20, 5, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (player.isOnFire() || player.level().getBlockState(player.blockPosition().below()).is(Blocks.LAVA)) {
                player.heal(6f);
                player.clearFire();
                notifyActivation(player, "§6Поглощение тепла! +3♥");
            } else {
                notifyActivation(player, "§cНет источника огня/лавы рядом!");
            }
        }
    }

    // 5. Взрывной бег — поджигает блоки и наносит урон (Ветка B)
    public static class ExplosiveRun extends AbstractAbility {
        public ExplosiveRun() {
            super(id("explosive_run"), "ability.racecraft.explosive_run.name",
                    "ability.racecraft.explosive_run.desc", 600, 20, 5, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) dur, 1));
            player.getPersistentData().putLong("explosive_run_expire", level.getGameTime() + (long) dur);
            // Поджигаем ближайших врагов
            AABB box = player.getBoundingBox().inflate(3);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> { e.setRemainingFireTicks(80); e.hurt(player.damageSources().playerAttack(player), 2f); });
            notifyActivation(player, "§6Взрывной бег!");
        }
    }

    // 6. Перегрузка — выброс накопленной энергии (Ветка B)
    public static class Overload extends AbstractAbility {
        public Overload() {
            super(id("overload"), "ability.racecraft.overload.name",
                    "ability.racecraft.overload.desc", 800, 20, 5, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            double blocks = player.getPersistentData().getDouble("pyro_blocks_walked");
            float damage = (float) Math.min(blocks / 2, 20) * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage));
            player.getPersistentData().putDouble("pyro_blocks_walked", 0);
            notifyActivation(player, "§6Перегрузка! Урон: " + (int) damage);
        }
    }

    // 7. Огненный след — пассивный огненный след (Tier 3)
    public static class FireTrail extends AbstractAbility {
        public FireTrail() {
            super(id("fire_trail"), "ability.racecraft.fire_trail.name",
                    "ability.racecraft.fire_trail.desc", 200, 30, 5, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.getPersistentData().putLong("fire_trail_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§6Огненный след активирован!");
        }
    }
}
