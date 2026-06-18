package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.mob.SummonedWolf;
import com.eternity.races.common.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 6 — Лунный Жнец: все 8 способностей */
public class LunarReaperAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Moonlight());
        reg.accept(new Shadow());
        reg.accept(new Cycle());
        reg.accept(new SoulReaper());
        reg.accept(new SleepingAgent());
        reg.accept(new LunarCall());
        reg.accept(new Eclipse());
        reg.accept(new LunarRegen());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    private static boolean isNight(Level level) { return !level.isDay(); }

    // 0. Лунный свет — луч урона (только ночью)
    public static class Moonlight extends AbstractAbility {
        public Moonlight() {
            super(id("moonlight"), "ability.racecraft.moonlight.name",
                    "ability.racecraft.moonlight.desc", 200, 0, 6, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!isNight(level)) { notifyActivation(player, "§cТолько ночью!"); return; }
            float dmg = 10f * getDamageMultiplier(player);
            Vec3 look = player.getLookAngle();
            AABB beam = player.getBoundingBox().expandTowards(look.scale(20));
            level.getEntitiesOfClass(LivingEntity.class, beam, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
            notifyActivation(player, "§5Лунный свет! 10 урона");
        }
    }

    // 1. Тень — невидимость
    public static class Shadow extends AbstractAbility {
        public Shadow() {
            super(id("shadow"), "ability.racecraft.shadow.name",
                    "ability.racecraft.shadow.desc", 300, 0, 6, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            int dur = (int) (160 * getDurationMultiplier(player));
            if (isNight(level)) dur = (int) (dur * 1.5f); // Ночью настоящая невидимость
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            notifyActivation(player, "§5Тень — невидимость!");
        }
    }

    // 2. Цикл — принудительная ночь
    public static class Cycle extends AbstractAbility {
        public Cycle() {
            super(id("cycle"), "ability.racecraft.cycle.name",
                    "ability.racecraft.cycle.desc", 6000, 0, 6, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!level.isClientSide) {
                ((net.minecraft.server.level.ServerLevel) level).setDayTime(13000);
                player.hurt(player.damageSources().magic(), 10f);
            }
            notifyActivation(player, "§5Цикл — наступила ночь! -10♥");
        }
    }

    // 3. Жнец душ — пассивный бонус за убийства (Ветка A)
    public static class SoulReaper extends AbstractAbility {
        public SoulReaper() {
            super(id("soul_reaper"), "ability.racecraft.soul_reaper.name",
                    "ability.racecraft.soul_reaper.desc", 0, 20, 6, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            // Пассивная: начисляется в RaceEventHandler при убийстве ночью
            notifyActivation(player, "§5Жнец душ активирован.");
        }
    }

    // 4. Спящий агент — сон → пробуждение ночью у кровати (Ветка A)
    public static class SleepingAgent extends AbstractAbility {
        public SleepingAgent() {
            super(id("sleeping_agent"), "ability.racecraft.sleeping_agent.name",
                    "ability.racecraft.sleeping_agent.desc", 3000, 20, 6, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0));
            player.getPersistentData().putBoolean("lunar_sleeping", true);
            notifyActivation(player, "§5Спящий агент — ожидание ночи...");
        }
    }

    // 5. Лунный зов — призыв 2 волков (Ветка B)
    public static class LunarCall extends AbstractAbility {
        public LunarCall() {
            super(id("lunar_call"), "ability.racecraft.lunar_call.name",
                    "ability.racecraft.lunar_call.desc", 1200, 20, 6, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!isNight(level)) { notifyActivation(player, "§cТолько ночью!"); return; }
            if (!level.isClientSide) {
                for (int i = 0; i < 2; i++) {
                    SummonedWolf wolf = ModEntities.SUMMONED_WOLF.get().create(level);
                    if (wolf != null) {
                        double ox = (level.random.nextDouble() - 0.5) * 4;
                        double oz = (level.random.nextDouble() - 0.5) * 4;
                        wolf.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                        wolf.setOwner(player);
                        wolf.setLifetime(1200); // 60 секунд
                        level.addFreshEntity(wolf);
                    }
                }
            }
            notifyActivation(player, "§5Лунный зов — волки призваны!");
        }
    }

    // 6. Затмение — гасит источники света (Ветка B)
    public static class Eclipse extends AbstractAbility {
        public Eclipse() {
            super(id("eclipse"), "ability.racecraft.eclipse.name",
                    "ability.racecraft.eclipse.desc", 800, 20, 6, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(16);
            level.getEntitiesOfClass(Player.class, box)
                    .forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, (int) dur, 0)));
            notifyActivation(player, "§5Затмение — темнота вокруг!");
        }
    }

    // 7. Лунная регенерация — пассивный реген ночью (Tier 3)
    public static class LunarRegen extends AbstractAbility {
        public LunarRegen() {
            super(id("lunar_regen"), "ability.racecraft.lunar_regen.name",
                    "ability.racecraft.lunar_regen.desc", 100, 30, 6, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (isNight(level)) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                notifyActivation(player, "§5Лунная регенерация!");
            } else {
                notifyActivation(player, "§cТолько ночью!");
            }
        }
    }
}
