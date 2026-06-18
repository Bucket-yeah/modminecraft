package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
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

    // 0. Звуковой удар — АоЕ 5 блоков, 4 урона, отбрасывание
    public static class SoundBurst extends AbstractAbility {
        public SoundBurst() {
            super(id("sound_burst"), "ability.racecraft.sound_burst.name",
                    "ability.racecraft.sound_burst.desc", 100, 0, 1, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            float damage = 4f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), damage);
                        double dx = e.getX() - player.getX();
                        double dz = e.getZ() - player.getZ();
                        double len = Math.sqrt(dx * dx + dz * dz);
                        if (len > 0) e.push(dx / len * 1.2, 0.4, dz / len * 1.2);
                    });
            if (level instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 20, 3, 0.5, 3, 0);
            notifyActivation(player, "§bЗвуковой удар!");
        }
    }

    // 1. Сейсмический прыжок — прыжок + ударная волна
    public static class SeismicJump extends AbstractAbility {
        public SeismicJump() {
            super(id("seismic_jump"), "ability.racecraft.seismic_jump.name",
                    "ability.racecraft.seismic_jump.desc", 200, 0, 1, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            player.push(0, 1.2, 0);
            player.setJumping(true);
            // При приземлении ударная волна обрабатывается в RaceEventHandler
            player.getPersistentData().putBoolean("echo_seismic_active", true);
            notifyActivation(player, "§bСейсмический прыжок!");
        }
    }

    // 2. Резонанс — блок наносит урон при касании
    public static class Resonance extends AbstractAbility {
        public Resonance() {
            super(id("resonance"), "ability.racecraft.resonance.name",
                    "ability.racecraft.resonance.desc", 400, 0, 1, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            // Помечаем блок перед игроком как резонирующий
            var pos = player.blockPosition().relative(player.getDirection());
            player.getPersistentData().putLong("resonance_block_x", pos.getX());
            player.getPersistentData().putLong("resonance_block_y", pos.getY());
            player.getPersistentData().putLong("resonance_block_z", pos.getZ());
            player.getPersistentData().putLong("resonance_expire", level.getGameTime() + 400);
            notifyActivation(player, "§bРезонанс активирован!");
        }
    }

    // 3. Глушитель (Ветка A) — поле тишины
    public static class Silencer extends AbstractAbility {
        public Silencer() {
            super(id("silencer"), "ability.racecraft.silencer.name",
                    "ability.racecraft.silencer.desc", 600, 20, 1, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) dur, 3));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 1));
                    });
            notifyActivation(player, "§bПоле тишины создано!");
        }
    }

    // 4. Эхолокация (Ветка A) — подсветка существ и руд
    public static class Echolocation extends AbstractAbility {
        public Echolocation() {
            super(id("echolocation"), "ability.racecraft.echolocation.name",
                    "ability.racecraft.echolocation.desc", 300, 20, 1, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(30);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§bЭхолокация — существа подсвечены!");
        }
    }

    // 5. Вибрационный щит (Ветка B) — отражение урона
    public static class VibrationShield extends AbstractAbility {
        public VibrationShield() {
            super(id("vibration_shield"), "ability.racecraft.vibration_shield.name",
                    "ability.racecraft.vibration_shield.desc", 400, 20, 1, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 80 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 1));
            player.getPersistentData().putLong("vibration_shield_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§bВибрационный щит активирован!");
        }
    }

    // 6. Звуковой взлом (Ветка B) — мгновенное разрушение камня
    public static class SonicBreak extends AbstractAbility {
        public SonicBreak() {
            super(id("sonic_break"), "ability.racecraft.sonic_break.name",
                    "ability.racecraft.sonic_break.desc", 300, 20, 1, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            var pos = player.blockPosition().relative(player.getDirection());
            var block = level.getBlockState(pos).getBlock();
            var tag = net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE;
            if (level.getBlockState(pos).is(tag)) {
                level.destroyBlock(pos, true, player);
                player.hurt(player.damageSources().magic(), 3f);
                notifyActivation(player, "§bЗвуковой взлом! -3♥");
            }
        }
    }

    // 7. Акустический ускоритель (Tier 3) — +40% урона звуком 10 сек
    public static class AcousticAccelerator extends AbstractAbility {
        public AcousticAccelerator() {
            super(id("acoustic_accelerator"), "ability.racecraft.acoustic_accelerator.name",
                    "ability.racecraft.acoustic_accelerator.desc", 800, 30, 1, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) dur, 1));
            player.getPersistentData().putLong("acoustic_boost_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§bАкустический ускоритель! Звуковые способности усилены!");
        }
    }
}
