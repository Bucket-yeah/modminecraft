package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 9 — Тень-Дипломат: все 8 способностей */
public class ShadowDiplomatAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Command());
        reg.accept(new Fear());
        reg.accept(new Illusion());
        reg.accept(new DarkContract());
        reg.accept(new GatherInfo());
        reg.accept(new HideInDarkness());
        reg.accept(new MassHypnosis());
        reg.accept(new ShadowSphere());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 9.1 Приказ — ближайший враг атакует других 30 сек (300t, 1 голод) */
    public static class Command extends AbstractAbility {
        public Command() {
            super(id("command"), "ability.racecraft.command.name",
                    "ability.racecraft.command.desc", 300, 0, 9, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(10);
            var mobs = level.getEntitiesOfClass(Mob.class, box, e -> !player.is(e));
            if (mobs.isEmpty()) { notifyActivation(player, "§cНет мобов рядом!"); return; }
            Mob target = mobs.get(0);
            target.setTarget(null);
            // Перенаправляем моба на другого врага
            level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(15), e -> !e.is(player) && !e.is(target))
                    .stream().findFirst().ifPresent(target::setTarget);
            player.getPersistentData().putBoolean("shadow_controlling", true);
            player.getPersistentData().putLong("shadow_control_expire", level.getGameTime() + 600);
            player.getPersistentData().putUUID("shadow_controlled_mob", target.getUUID());
            notifyActivation(player, "§2Приказ! Моб " + target.getName().getString() + " переключён.");
        }
    }

    /** 9.2 Страх — паника 8 сек в радиусе 10 (400t, 1 голод) */
    public static class Fear extends AbstractAbility {
        public Fear() {
            super(id("fear"), "ability.racecraft.fear.name",
                    "ability.racecraft.fear.desc", 400, 0, 9, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 10f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(160 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
                        if (e instanceof Mob mob) mob.setTarget(null);
                    });
            notifyActivation(player, "§2Страх! Враги в панике 8 сек.");
        }
    }

    /** 9.3 Иллюзия — невидимость 10 сек (200t, 1 голод) */
    public static class Illusion extends AbstractAbility {
        public Illusion() {
            super(id("illusion"), "ability.racecraft.illusion.name",
                    "ability.racecraft.illusion.desc", 200, 0, 9, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(200 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            player.getPersistentData().putLong("illusion_expire", level.getGameTime() + dur);
            notifyActivation(player, "§2Иллюзия! Невидимость 10 сек.");
        }
    }

    /** 9.4 Тёмный договор — союз с 3 мобами, они не атакуют 2 мин (2400t, 2 голода) */
    public static class DarkContract extends AbstractAbility {
        public DarkContract() {
            super(id("dark_contract"), "ability.racecraft.dark_contract.name",
                    "ability.racecraft.dark_contract.desc", 2400, 20, 9, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            float radius = 15f;
            int dur = (int)(2400 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            var mobs = level.getEntitiesOfClass(Mob.class, box, m -> {
                if (!m.is(player)) { m.setTarget(null); return true; }
                return false;
            });
            int count = Math.min(mobs.size(), 3 + getAccessoryLevel(player));
            player.getPersistentData().putInt("dark_contract_count", count);
            player.getPersistentData().putLong("dark_contract_expire", level.getGameTime() + dur);
            notifyActivation(player, "§2Тёмный договор! " + count + " мобов не атакуют 2 мин.");
        }
    }

    /** 9.5 Сбор информации — статус ближайшего моба (600t, 1 голод) */
    public static class GatherInfo extends AbstractAbility {
        public GatherInfo() {
            super(id("gather_info"), "ability.racecraft.gather_info.name",
                    "ability.racecraft.gather_info.desc", 600, 20, 9, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(20);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей в 20 блоках!"); return; }
            LivingEntity target = targets.stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player))).get();
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            String info = "§2" + target.getName().getString() +
                    " §7HP: " + (int) target.getHealth() + "/" + (int) target.getMaxHealth() +
                    " Расст: " + (int) target.distanceTo(player) + "блк";
            if (player instanceof net.minecraft.server.level.ServerPlayer sp)
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(info));
            notifyActivation(player, info);
        }
    }

    /** 9.6 Укрытие в темноте — инвизибилити в темноте 15 сек (500t, 1 голод) */
    public static class HideInDarkness extends AbstractAbility {
        public HideInDarkness() {
            super(id("hide_in_darkness"), "ability.racecraft.hide_in_darkness.name",
                    "ability.racecraft.hide_in_darkness.desc", 500, 20, 9, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int light = level.getMaxLocalRawBrightness(player.blockPosition());
            if (light > 7) { notifyActivation(player, "§cНужна темнота (освещение ≤7)!"); return; }
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0));
            notifyActivation(player, "§2Укрытие в темноте! +невидимость +скорость 15 сек.");
        }
    }

    /** 9.7 Массовый гипноз — дезориентация всех в 20 блоках 10 сек (1200t, 2 голода) */
    public static class MassHypnosis extends AbstractAbility {
        public MassHypnosis() {
            super(id("mass_hypnosis"), "ability.racecraft.mass_hypnosis.name",
                    "ability.racecraft.mass_hypnosis.desc", 1200, 20, 9, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            float radius = 20f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(200 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
                        if (e instanceof Mob mob) mob.setTarget(null);
                    });
            notifyActivation(player, "§2Массовый гипноз! Все в 20 блоках дезориентированы 10 сек.");
        }
    }

    /** 9.8 Сфера теней — сфера невидимости 5 блоков 15 сек (900t, 2 голода) */
    public static class ShadowSphere extends AbstractAbility {
        public ShadowSphere() {
            super(id("shadow_sphere"), "ability.racecraft.shadow_sphere.name",
                    "ability.racecraft.shadow_sphere.desc", 900, 30, 9, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            // Невидимость себе и союзникам рядом
            AABB box = player.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(Player.class, box)
                    .forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0)));
            // Все мобы в сфере теряют цель
            level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(5))
                    .forEach(m -> m.setTarget(null));
            notifyActivation(player, "§2Сфера теней! Невидимость 5 блоков на 15 сек.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
