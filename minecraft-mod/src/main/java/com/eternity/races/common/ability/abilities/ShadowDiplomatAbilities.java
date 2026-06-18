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

/** Раса 9 — Теневой Дипломат: все 8 способностей */
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

    // 0. Приказ — управление мобом 30 сек
    public static class Command extends AbstractAbility {
        public Command() {
            super(id("command"), "ability.racecraft.command.name",
                    "ability.racecraft.command.desc", 1200, 0, 9, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(Mob.class, box, e -> e != player).stream().findFirst()
                    .ifPresent(mob -> {
                        mob.setTarget(null);
                        mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
                        player.getPersistentData().putUUID("diplomat_controlled_mob", mob.getUUID());
                        player.getPersistentData().putBoolean("shadow_controlling", true);
                    });
            notifyActivation(player, "§8Приказ — моб подчинён!");
        }
    }

    // 1. Страх — бегство врагов
    public static class Fear extends AbstractAbility {
        public Fear() {
            super(id("fear"), "ability.racecraft.fear.name",
                    "ability.racecraft.fear.desc", 400, 0, 9, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(Mob.class, box, e -> e != player)
                    .forEach(mob -> {
                        mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) dur, 0));
                        mob.setTarget(null);
                        // Толкаем от игрока
                        var away = mob.position().subtract(player.position()).normalize();
                        mob.push(away.x * 2, 0.4, away.z * 2);
                    });
            notifyActivation(player, "§8Страх — враги бегут!");
        }
    }

    // 2. Иллюзия — создание отвлекающей копии
    public static class Illusion extends AbstractAbility {
        public Illusion() {
            super(id("illusion"), "ability.racecraft.illusion.name",
                    "ability.racecraft.illusion.desc", 600, 0, 9, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            // Создаём временную приманку через ArmorStand
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level,
                    player.getX() + 3, player.getY(), player.getZ() + 3);
            stand.setCustomName(player.getName());
            stand.setCustomNameVisible(true);
            level.addFreshEntity(stand);
            // Удалим через 10 сек в тик-обработчике
            stand.getPersistentData().putLong("illusion_expire", level.getGameTime() + 200);
            notifyActivation(player, "§8Иллюзия создана!");
        }
    }

    // 3. Тёмный договор — обмен телами (Ветка A)
    public static class DarkContract extends AbstractAbility {
        public DarkContract() {
            super(id("dark_contract"), "ability.racecraft.dark_contract.name",
                    "ability.racecraft.dark_contract.desc", 800, 20, 9, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(Mob.class, box, e -> e != player).stream().findFirst()
                    .ifPresent(mob -> {
                        // Меняем местами позиции
                        double px = player.getX(), py = player.getY(), pz = player.getZ();
                        player.teleportTo(mob.getX(), mob.getY(), mob.getZ());
                        mob.teleportTo(px, py, pz);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
                        notifyActivation(player, "§8Тёмный договор — обмен позициями!");
                    });
        }
    }

    // 4. Сбор информации — просмотр здоровья/инвентаря (Ветка A)
    public static class GatherInfo extends AbstractAbility {
        public GatherInfo() {
            super(id("gather_info"), "ability.racecraft.gather_info.name",
                    "ability.racecraft.gather_info.desc", 300, 20, 9, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§8Сбор информации — враги подсвечены!");
        }
    }

    // 5. Укрытие во тьме — полная невидимость 15 сек (Ветка B)
    public static class HideInDarkness extends AbstractAbility {
        public HideInDarkness() {
            super(id("hide_in_darkness"), "ability.racecraft.hide_in_darkness.name",
                    "ability.racecraft.hide_in_darkness.desc", 600, 20, 9, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 300 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) dur, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, (int) dur, 0));
            notifyActivation(player, "§8Укрытие во тьме — полная невидимость!");
        }
    }

    // 6. Массовый гипноз — паралич врагов (Ветка B)
    public static class MassHypnosis extends AbstractAbility {
        public MassHypnosis() {
            super(id("mass_hypnosis"), "ability.racecraft.mass_hypnosis.name",
                    "ability.racecraft.mass_hypnosis.desc", 800, 20, 9, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 160 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(12);
            level.getEntitiesOfClass(Mob.class, box, e -> e != player)
                    .forEach(mob -> {
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 127));
                        mob.setTarget(null);
                    });
            notifyActivation(player, "§8Массовый гипноз — все враги парализованы!");
        }
    }

    // 7. Теневая сфера — зона тьмы (Tier 3)
    public static class ShadowSphere extends AbstractAbility {
        public ShadowSphere() {
            super(id("shadow_sphere"), "ability.racecraft.shadow_sphere.name",
                    "ability.racecraft.shadow_sphere.desc", 600, 30, 9, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 240 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(12);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, (int) dur, 1)));
            notifyActivation(player, "§8Теневая сфера — тьма поглощает всё!");
        }
    }
}
