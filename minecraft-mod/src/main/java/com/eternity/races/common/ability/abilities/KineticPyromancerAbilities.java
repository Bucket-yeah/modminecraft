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

    /** 5.1 Разгон — +60% скорость 10 сек, затем -30% 5 сек (300t, 1 голод) */
    public static class Acceleration extends AbstractAbility {
        public Acceleration() {
            super(id("acceleration"), "ability.racecraft.acceleration.name",
                    "ability.racecraft.acceleration.desc", 300, 0, 5, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            int lvl = getAccessoryLevel(player);
            // Speed 2 = ~+60%, дополнительно +5% за уровень кольца
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 2 + lvl / 5));
            // Замедление после
            player.getPersistentData().putLong("pyro_slow_start", level.getGameTime() + dur);
            notifyActivation(player, "§6Разгон! +60% скорость на 10 сек.");
        }
    }

    /** 5.2 Искра — огненный шар (160t, 1 голод) */
    public static class Spark extends AbstractAbility {
        public Spark() {
            super(id("spark"), "ability.racecraft.spark.name",
                    "ability.racecraft.spark.desc", 160, 0, 5, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            Vec3 look = player.getLookAngle();
            // Урон = 5 + блоки пройденные/2 (макс +10), масштабируется
            double blocksWalked = player.getPersistentData().getDouble("pyro_blocks_walked");
            float bonusDmg = (float) Math.min(blocksWalked / 2.0, 10.0);
            float baseDmg = (5f + bonusDmg) * getDamageMultiplier(player);
            var fireball = new net.minecraft.world.entity.projectile.SmallFireball(level, player,
                    look.scale(1.5));
            fireball.setPos(player.getX() + look.x * 2, player.getY() + 1.5, player.getZ() + look.z * 2);
            level.addFreshEntity(fireball);
            notifyActivation(player, "§6Искра! Урон ~" + (int) baseDmg);
        }
    }

    /** 5.3 Термо-щит — огненный барьер 10 сек, 2 урона/сек врагам (400t, 1 голод) */
    public static class ThermoShield extends AbstractAbility {
        public ThermoShield() {
            super(id("thermo_shield"), "ability.racecraft.thermo_shield.name",
                    "ability.racecraft.thermo_shield.desc", 400, 0, 5, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur + 200, 0));
            player.getPersistentData().putLong("thermo_shield_expire", level.getGameTime() + dur);
            notifyActivation(player, "§6Термо-щит! Враги в 3 блоках горят 10 сек.");
        }
    }

    /** 5.4 Кинетический удар — урон = скорость/2 (60t, 0.5 голода) */
    public static class KineticStrike extends AbstractAbility {
        public KineticStrike() {
            super(id("kinetic_strike"), "ability.racecraft.kinetic_strike.name",
                    "ability.racecraft.kinetic_strike.desc", 60, 20, 5, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            // 0.5 голода — добавляем усталость (не минус голода напрямую)
            player.getFoodData().addExhaustion(2f);
            double speed = player.getDeltaMovement().horizontalDistance();
            int lvl = getAccessoryLevel(player);
            float mult = 1f + lvl * 0.1f;
            float damage = Math.max(1f, (float) (speed * 10 * mult * getDamageMultiplier(player)));
            damage = Math.min(damage, 15f);
            AABB box = player.getBoundingBox().inflate(2).inflate(0, 1, 0);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> !player.is(en))) {
                e.hurt(player.damageSources().playerAttack(player), damage);
                hit = true;
            }
            if (hit) notifyActivation(player, "§6Кинетический удар! Урон: " + (int) damage);
            else notifyActivation(player, "§cНет врагов рядом!");
        }
    }

    /** 5.5 Поглощение тепла — лечение от огня/лавы +3♥, бонус урона (300t, 1 голод) */
    public static class HeatAbsorption extends AbstractAbility {
        public HeatAbsorption() {
            super(id("heat_absorption"), "ability.racecraft.heat_absorption.name",
                    "ability.racecraft.heat_absorption.desc", 300, 20, 5, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int lvl = getAccessoryLevel(player);
            float heal = (3f + lvl / 2f);
            // Проверяем огонь/лаву рядом
            AABB box = player.getBoundingBox().inflate(5);
            boolean found = false;
            for (BlockPos pos : BlockPos.betweenClosed(
                    BlockPos.containing(box.minX, box.minY, box.minZ),
                    BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                var bs = level.getBlockState(pos);
                if (bs.is(Blocks.FIRE) || bs.is(Blocks.LAVA) || bs.is(Blocks.CAMPFIRE)) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    found = true;
                    break;
                }
            }
            if (player.isOnFire()) { player.clearFire(); found = true; }
            player.heal(heal);
            float dmgBonus = 2f + lvl * 0.5f;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, (int)(dmgBonus / 2)));
            notifyActivation(player, "§6Поглощение тепла! +" + (int) heal + "♥" + (found ? ", огонь поглощён" : ""));
        }
    }

    /** 5.6 Взрывной бег — поджигает блоки и врагов 15 сек (800t, 3 голода) */
    public static class ExplosiveRun extends AbstractAbility {
        public ExplosiveRun() {
            super(id("explosive_run"), "ability.racecraft.explosive_run.name",
                    "ability.racecraft.explosive_run.desc", 800, 20, 5, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1));
            player.getPersistentData().putLong("explosive_run_expire", level.getGameTime() + dur);
            // Поджигаем ближайших врагов сразу
            float dmg = 2f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(3);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.setRemainingFireTicks(100);
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                    });
            notifyActivation(player, "§6Взрывной бег! Поджигаешь врагов 15 сек.");
        }
    }

    /** 5.7 Перегрузка — урон = пройденные блоки/2, макс 20 (1800t, 2 голода) */
    public static class Overload extends AbstractAbility {
        public Overload() {
            super(id("overload"), "ability.racecraft.overload.name",
                    "ability.racecraft.overload.desc", 1800, 20, 5, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            double blocks = player.getPersistentData().getDouble("pyro_blocks_walked");
            int lvl = getAccessoryLevel(player);
            float maxDmg = 20f + lvl * 2f;
            float damage = (float) Math.min(blocks / 2.0, maxDmg) * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), damage));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
            player.getPersistentData().putDouble("pyro_blocks_walked", 0);
            notifyActivation(player, "§6Перегрузка! Урон: " + (int) damage + " всем в 8 блоках.");
        }
    }

    /** 5.8 Огненный след — пассивный огненный след при беге (0t, пассив) */
    public static class FireTrail extends AbstractAbility {
        public FireTrail() {
            super(id("fire_trail"), "ability.racecraft.fire_trail.name",
                    "ability.racecraft.fire_trail.desc", 0, 30, 5, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            // Пассивная — включается/выключается
            boolean active = player.getPersistentData().getBoolean("fire_trail_passive");
            player.getPersistentData().putBoolean("fire_trail_passive", !active);
            notifyActivation(player, "§6Огненный след: " + (!active ? "§aвкл" : "§cвыкл"));
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
