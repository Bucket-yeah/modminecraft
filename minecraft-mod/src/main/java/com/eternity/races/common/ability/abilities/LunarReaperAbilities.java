package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 6 — Лунный Жнец: все 8 способностей */
public class LunarReaperAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new MoonlightBlade());
        reg.accept(new Shadow());
        reg.accept(new LunarCycle());
        reg.accept(new SoulReaper());
        reg.accept(new SleepingAgent());
        reg.accept(new LunarCall());
        reg.accept(new Eclipse());
        reg.accept(new LunarRegen());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 6.1 Лунный клинок — урон ×3 ночью, ×1.5 днём (200t, 1 голод) */
    public static class MoonlightBlade extends AbstractAbility {
        public MoonlightBlade() {
            super(id("moonlight_blade"), "ability.racecraft.moonlight_blade.name",
                    "ability.racecraft.moonlight_blade.desc", 200, 0, 6, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            boolean night = !level.isDay();
            float mult = (night ? 3f : 1.5f) * getDamageMultiplier(player);
            float damage = 5f * mult * (1f + getAccessoryLevel(player) * 0.05f);
            AABB box = player.getBoundingBox().inflate(3);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, en -> !player.is(en))) {
                e.hurt(player.damageSources().playerAttack(player), damage);
                hit = true;
            }
            if (hit) notifyActivation(player, night ? "§5Лунный клинок! ×3 урон." : "§5Лунный клинок! ×1.5 урон.");
            else notifyActivation(player, "§cНет врагов рядом!");
        }
    }

    /** 6.2 Тень — невидимость 10 сек, +20% скорость (только ночью) (400t, 1 голод) */
    public static class Shadow extends AbstractAbility {
        public Shadow() {
            super(id("shadow"), "ability.racecraft.shadow.name",
                    "ability.racecraft.shadow.desc", 400, 0, 6, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            if (level.isDay()) { notifyActivation(player, "§cТолько ночью!"); return; }
            int dur = (int)(200 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0));
            notifyActivation(player, "§5Тень! Невидимость + скорость 10 сек.");
        }
    }

    /** 6.3 Лунный цикл — смена времени суток, -10♥ (6000t, 10 HP) */
    public static class LunarCycle extends AbstractAbility {
        public LunarCycle() {
            super(id("lunar_cycle"), "ability.racecraft.lunar_cycle.name",
                    "ability.racecraft.lunar_cycle.desc", 6000, 0, 6, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (player.getHealth() <= 10f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
            player.hurt(player.damageSources().magic(), 10f);
            if (level instanceof ServerLevel sl) {
                long time = sl.getDayTime() % 24000L;
                if (sl.isDay()) sl.setDayTime((sl.getDayTime() / 24000L) * 24000L + 13000L);
                else sl.setDayTime(((sl.getDayTime() / 24000L) + 1) * 24000L);
            }
            notifyActivation(player, "§5Лунный цикл — смена времени! -10♥");
        }
    }

    /** 6.4 Жнец душ — пассив: бонус XP за ночные убийства (0t, пассив) */
    public static class SoulReaper extends AbstractAbility {
        public SoulReaper() {
            super(id("soul_reaper"), "ability.racecraft.soul_reaper.name",
                    "ability.racecraft.soul_reaper.desc", 0, 20, 6, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            notifyActivation(player, "§5Жнец душ: пассивный бонус XP ночью активен.");
        }
    }

    /** 6.5 Снотворный агент — ближайший враг в 10 блоках засыпает 5 сек (1200t, ничего) */
    public static class SleepingAgent extends AbstractAbility {
        public SleepingAgent() {
            super(id("sleeping_agent"), "ability.racecraft.sleeping_agent.name",
                    "ability.racecraft.sleeping_agent.desc", 1200, 20, 6, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(10);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей!"); return; }
            int dur = (int)(100 * getDurationMultiplier(player));
            targets.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .ifPresent(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 127));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 127));
                        if (e instanceof net.minecraft.world.entity.Mob mob) mob.setTarget(null);
                    });
            notifyActivation(player, "§5Снотворный агент — цель обездвижена на 5 сек!");
        }
    }

    /** 6.6 Лунный зов — призыв 3+ волков-теней (2400t, 2 голода) */
    public static class LunarCall extends AbstractAbility {
        public LunarCall() {
            super(id("lunar_call"), "ability.racecraft.lunar_call.name",
                    "ability.racecraft.lunar_call.desc", 2400, 20, 6, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            if (!level.isClientSide) {
                int count = 3 + getAccessoryLevel(player) / 3;
                for (int i = 0; i < Math.min(count, 6); i++) {
                    com.eternity.races.common.mob.SummonedWolf wolf =
                            com.eternity.races.common.registry.ModEntities.SUMMONED_WOLF.get().create(level);
                    if (wolf != null) {
                        double ox = (level.random.nextDouble() - 0.5) * 4;
                        double oz = (level.random.nextDouble() - 0.5) * 4;
                        wolf.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                        wolf.setOwner(player);
                        wolf.setLifetime(1200);
                        level.addFreshEntity(wolf);
                    }
                }
            }
            notifyActivation(player, "§5Лунный зов! Волки-тени призваны.");
        }
    }

    /** 6.7 Затмение — ослабление врагов в 30 блоках, бафф себе (1800t, 2 голода) */
    public static class Eclipse extends AbstractAbility {
        public Eclipse() {
            super(id("eclipse"), "ability.racecraft.eclipse.name",
                    "ability.racecraft.eclipse.desc", 1800, 20, 6, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int dur = (int)(400 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(30);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 0));
                    });
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1));
            notifyActivation(player, "§5Затмение! Враги ослаблены, ты усилен.");
        }
    }

    /** 6.8 Лунная регенерация — пассив (0t, пассив) */
    public static class LunarRegen extends AbstractAbility {
        public LunarRegen() {
            super(id("lunar_regen"), "ability.racecraft.lunar_regen.name",
                    "ability.racecraft.lunar_regen.desc", 0, 30, 6, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            notifyActivation(player, "§5Лунная регенерация: пассивный реген HP ночью.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
