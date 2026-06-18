package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 1 — Эхо-Голем: все 8 способностей */
public class EchoGolemAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new SoundBurst());
        reg.accept(new SeismicJump());
        reg.accept(new Resonance());
        reg.accept(new Silencer());
        reg.accept(new Echolocation());
        reg.accept(new VibrationShield());
        reg.accept(new SonicBreak());
        reg.accept(new AcousticAccelerator());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 1.1 Звуковой удар — АоЕ 5 блоков, 4 урона, отбрасывание (100t, 2 голода) */
    public static class SoundBurst extends AbstractAbility {
        public SoundBurst() {
            super(id("sound_burst"), "ability.racecraft.sound_burst.name",
                    "ability.racecraft.sound_burst.desc", 100, 0, 1, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            float radius = 5f * (1f + getAccessoryLevel(player) * 0.02f);
            if (isWeakened(player)) radius *= 0.7f;
            float damage = 4f * getDamageMultiplier(player);
            if (isWeakened(player)) damage *= 0.7f;
            final float finalDamage = damage;
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), finalDamage);
                        double dx = e.getX() - player.getX();
                        double dz = e.getZ() - player.getZ();
                        double len = Math.sqrt(dx * dx + dz * dz);
                        if (len > 0) e.push(dx / len * 1.5, 0.4, dz / len * 1.5);
                    });
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 20, radius / 2, 0.5, radius / 2, 0);
            notifyActivation(player, "§bЗвуковой удар!");
        }
    }

    /** 1.2 Сейсмический прыжок — прыжок + ударная волна при приземлении (200t, 3 голода) */
    public static class SeismicJump extends AbstractAbility {
        public SeismicJump() {
            super(id("seismic_jump"), "ability.racecraft.seismic_jump.name",
                    "ability.racecraft.seismic_jump.desc", 200, 0, 1, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            player.push(0, 1.5, 0);
            player.getPersistentData().putBoolean("echo_seismic_active", true);
            notifyActivation(player, "§bСейсмический прыжок!");
        }
    }

    /** 1.3 Резонанс — блок наносит урон при касании 20 сек (600t, 1 голод) */
    public static class Resonance extends AbstractAbility {
        public Resonance() {
            super(id("resonance"), "ability.racecraft.resonance.name",
                    "ability.racecraft.resonance.desc", 600, 0, 1, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            long dur = (long)(400 * getDurationMultiplier(player));
            BlockPos pos = player.blockPosition().relative(player.getDirection());
            if (!level.getBlockState(pos).isAir()) {
                player.getPersistentData().putInt("resonance_x", pos.getX());
                player.getPersistentData().putInt("resonance_y", pos.getY());
                player.getPersistentData().putInt("resonance_z", pos.getZ());
                player.getPersistentData().putLong("resonance_expire", level.getGameTime() + dur);
                notifyActivation(player, "§bРезонанс! Блок станет опасным на 20 сек.");
            } else {
                notifyActivation(player, "§cПрицелься на блок!");
            }
        }
    }

    /** 1.4 Глушитель — поле тишины 6 блоков, 15 сек (900t, 1 голод) */
    public static class Silencer extends AbstractAbility {
        public Silencer() {
            super(id("silencer"), "ability.racecraft.silencer.name",
                    "ability.racecraft.silencer.desc", 900, 20, 1, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 6f * (1f + getAccessoryLevel(player) * 0.03f);
            int dur = (int)(300 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0, false, false));
                        if (e instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(null);
                    });
            player.getPersistentData().putLong("silencer_expire", level.getGameTime() + dur);
            notifyActivation(player, "§bПоле тишины создано на 15 сек!");
        }
    }

    /** 1.5 Эхолокация — подсветка существ и руд 30 блоков, 5 сек (400t, 2 голода) */
    public static class Echolocation extends AbstractAbility {
        public Echolocation() {
            super(id("echolocation"), "ability.racecraft.echolocation.name",
                    "ability.racecraft.echolocation.desc", 400, 20, 1, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            float radius = 30f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(100 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false)));
            notifyActivation(player, "§bЭхолокация — существа подсвечены!");
        }
    }

    /** 1.6 Вибрационный щит — отражение 50% урона 8 сек (500t, 2 голода) */
    public static class VibrationShield extends AbstractAbility {
        public VibrationShield() {
            super(id("vibration_shield"), "ability.racecraft.vibration_shield.name",
                    "ability.racecraft.vibration_shield.desc", 500, 20, 1, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int dur = (int)(160 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.getPersistentData().putLong("vibration_shield_expire", level.getGameTime() + dur);
            notifyActivation(player, "§bВибрационный щит! Отражение 50% урона на 8 сек.");
        }
    }

    /** 1.7 Звуковой взлом — мгновенное разрушение камня, -3♥ (1200t, 2 голода) */
    public static class SonicBreak extends AbstractAbility {
        public SonicBreak() {
            super(id("sonic_break"), "ability.racecraft.sonic_break.name",
                    "ability.racecraft.sonic_break.desc", 1200, 20, 1, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            BlockPos pos = player.blockPosition().relative(player.getDirection());
            var state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
                level.destroyBlock(pos, true, player);
                float selfDmg = 3f * (1f - getAccessoryLevel(player) * 0.02f);
                player.hurt(player.damageSources().magic(), Math.max(0.5f, selfDmg));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 2));
                notifyActivation(player, "§bЗвуковой взлом! -3♥ себе");
            } else {
                notifyActivation(player, "§cПрицелься на блок (не бедрок)!");
            }
        }
    }

    /** 1.8 Акустический ускоритель — +40% урона звуком 10 сек (800t, 1 голод) */
    public static class AcousticAccelerator extends AbstractAbility {
        public AcousticAccelerator() {
            super(id("acoustic_accelerator"), "ability.racecraft.acoustic_accelerator.name",
                    "ability.racecraft.acoustic_accelerator.desc", 800, 30, 1, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1));
            player.getPersistentData().putLong("acoustic_boost_expire", level.getGameTime() + dur);
            notifyActivation(player, "§bАкустический ускоритель! +40% урон 10 сек.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
