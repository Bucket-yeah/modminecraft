package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 8 — Энтропийный Разрушитель: все 8 способностей */
public class EntropyDestroyerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Decay());
        reg.accept(new ChaosStrike());
        reg.accept(new EntropyExplosion());
        reg.accept(new Aging());
        reg.accept(new ArmorBreak());
        reg.accept(new BriefStability());
        reg.accept(new Annihilation());
        reg.accept(new MatterDecay());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 8.1 Разложение — наложение иссушения на ближайших (40t, 1 голод) */
    public static class Decay extends AbstractAbility {
        public Decay() {
            super(id("decay"), "ability.racecraft.decay.name",
                    "ability.racecraft.decay.desc", 40, 0, 8, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 4f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(100 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, dur, 1)));
            notifyActivation(player, "§8Разложение! Иссушение II в радиусе 4 блоков.");
        }
    }

    /** 8.2 Хаотичный удар — случайный урон 1–20 (160t, 1 голод) */
    public static class ChaosStrike extends AbstractAbility {
        public ChaosStrike() {
            super(id("chaos_strike"), "ability.racecraft.chaos_strike.name",
                    "ability.racecraft.chaos_strike.desc", 160, 0, 8, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float dmg = (level.random.nextFloat() * 19f + 1f) * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(4);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> !player.is(en))) {
                e.hurt(player.damageSources().playerAttack(player), dmg);
                hit = true;
            }
            if (hit) notifyActivation(player, "§8Хаотичный удар! Урон: " + (int) dmg);
            else notifyActivation(player, "§cНет врагов рядом!");
        }
    }

    /** 8.3 Энтропийный взрыв — АоЕ 10 блоков, 6 урона (600t, 2 голода) */
    public static class EntropyExplosion extends AbstractAbility {
        public EntropyExplosion() {
            super(id("entropy_explosion"), "ability.racecraft.entropy_explosion.name",
                    "ability.racecraft.entropy_explosion.desc", 600, 0, 8, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            float radius = 10f * (1f + getAccessoryLevel(player) * 0.05f);
            float dmg = 6f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        Vec3 dir = e.position().subtract(player.position()).normalize();
                        e.push(dir.x * 2, 0.5, dir.z * 2);
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                    });
            level.explode(player, player.getX(), player.getY(), player.getZ(), 1f, false, Level.ExplosionInteraction.NONE);
            notifyActivation(player, "§8Энтропийный взрыв! Урон " + (int) dmg + " в радиусе 10.");
        }
    }

    /** 8.4 Старение — цель теряет 50% скорости, −40% урон 12 сек (800t, 1 голод) */
    public static class Aging extends AbstractAbility {
        public Aging() {
            super(id("aging"), "ability.racecraft.aging.name",
                    "ability.racecraft.aging.desc", 800, 20, 8, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(8);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей!"); return; }
            int dur = (int)(240 * getDurationMultiplier(player));
            targets.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 3));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, 2));
                    });
            notifyActivation(player, "§8Старение! Цель ослаблена на 12 сек.");
        }
    }

    /** 8.5 Слом брони — разрушение защиты цели 10 сек (500t, 1 голод) */
    public static class ArmorBreak extends AbstractAbility {
        public ArmorBreak() {
            super(id("armor_break"), "ability.racecraft.armor_break.name",
                    "ability.racecraft.armor_break.desc", 500, 20, 8, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(6);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей!"); return; }
            int dur = (int)(200 * getDurationMultiplier(player));
            targets.forEach(e -> {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 4));
                // Повреждаем броню цели
                if (e instanceof net.minecraft.world.entity.player.Player tp) {
                    for (net.minecraft.world.item.ItemStack armor : tp.getArmorSlots()) {
                        if (!armor.isEmpty()) armor.hurtAndBreak(20, tp, net.minecraft.world.entity.EquipmentSlot.CHEST);
                    }
                }
            });
            notifyActivation(player, "§8Слом брони! -50% защита на 10 сек.");
        }
    }

    /** 8.6 Кратковременная стабильность — можно ставить блоки 20 сек (400t, 1 голод) */
    public static class BriefStability extends AbstractAbility {
        public BriefStability() {
            super(id("brief_stability"), "ability.racecraft.brief_stability.name",
                    "ability.racecraft.brief_stability.desc", 400, 20, 8, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(400 * getDurationMultiplier(player));
            player.getPersistentData().putLong("entropy_stable_expire", level.getGameTime() + dur);
            notifyActivation(player, "§8Стабильность! Можно строить " + (dur / 20) + " сек.");
        }
    }

    /** 8.7 Аннигиляция — уничтожение всего в 3 блоках, -5♥ (300t, 1 голод) */
    public static class Annihilation extends AbstractAbility {
        public Annihilation() {
            super(id("annihilation"), "ability.racecraft.annihilation.name",
                    "ability.racecraft.annihilation.desc", 300, 20, 8, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            if (player.getHealth() <= 5f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
            player.hurt(player.damageSources().magic(), 5f);
            float dmg = 15f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(3);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
            notifyActivation(player, "§8Аннигиляция! Урон " + (int) dmg + " в 3 блоках. -5♥");
        }
    }

    /** 8.8 Распад материи — снаряд разложения (200t, 1 голод) */
    public static class MatterDecay extends AbstractAbility {
        public MatterDecay() {
            super(id("matter_decay"), "ability.racecraft.matter_decay.name",
                    "ability.racecraft.matter_decay.desc", 200, 30, 8, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float dmg = 4f * getDamageMultiplier(player);
            net.minecraft.world.phys.Vec3 look = player.getLookAngle();
            // Урон по всем на пути луча
            net.minecraft.world.phys.Vec3 start = player.getEyePosition();
            net.minecraft.world.phys.Vec3 end = start.add(look.scale(20));
            AABB scanBox = new AABB(
                    Math.min(start.x, end.x) - 0.5, Math.min(start.y, end.y) - 0.5, Math.min(start.z, end.z) - 0.5,
                    Math.max(start.x, end.x) + 0.5, Math.max(start.y, end.y) + 0.5, Math.max(start.z, end.z) + 0.5);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, scanBox, en -> !player.is(en))) {
                e.hurt(player.damageSources().playerAttack(player), dmg);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                hit = true;
            }
            if (!hit) notifyActivation(player, "§8Распад материи — снаряд выпущен!");
            else notifyActivation(player, "§8Распад материи! Урон " + (int) dmg + " + иссушение.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
