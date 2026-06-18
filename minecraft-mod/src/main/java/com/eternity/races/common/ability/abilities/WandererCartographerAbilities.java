package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 12 — Странник-Картограф: все 8 способностей */
public class WandererCartographerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new SaveCoords());
        reg.accept(new TeleportToPoint());
        reg.accept(new BiomeAnalysis());
        reg.accept(new TempTrail());
        reg.accept(new PortalLoop());
        reg.accept(new Intuition());
        reg.accept(new Return());
        reg.accept(new Orientation());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Запись координат — сохранение до 5 точек
    public static class SaveCoords extends AbstractAbility {
        public SaveCoords() {
            super(id("save_coords"), "ability.racecraft.save_coords.name",
                    "ability.racecraft.save_coords.desc", 60, 0, 12, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            var data = player.getPersistentData();
            for (int i = 0; i < 5; i++) {
                if (!data.contains("wander_pt_" + i + "_x")) {
                    data.putDouble("wander_pt_" + i + "_x", player.getX());
                    data.putDouble("wander_pt_" + i + "_y", player.getY());
                    data.putDouble("wander_pt_" + i + "_z", player.getZ());
                    player.sendSystemMessage(Component.literal("§bКоординаты сохранены в слоте " + (i + 1) + ": " +
                            (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ()));
                    return;
                }
            }
            notifyActivation(player, "§cВсе 5 слотов заняты!");
        }
    }

    // 1. Телепорт к точке — прыжок к сохранённой точке
    public static class TeleportToPoint extends AbstractAbility {
        public TeleportToPoint() {
            super(id("teleport_to_point"), "ability.racecraft.teleport_to_point.name",
                    "ability.racecraft.teleport_to_point.desc", 300, 0, 12, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            var data = player.getPersistentData();
            // Телепортируемся к последней сохранённой точке
            for (int i = 4; i >= 0; i--) {
                if (data.contains("wander_pt_" + i + "_x")) {
                    // 10% ошибка: телепорт на 10 блоков вверх
                    if (Math.random() < 0.1) {
                        player.teleportTo(
                                data.getDouble("wander_pt_" + i + "_x"),
                                data.getDouble("wander_pt_" + i + "_y") + 10,
                                data.getDouble("wander_pt_" + i + "_z"));
                        notifyActivation(player, "§cОшибка телепортации!");
                    } else {
                        player.teleportTo(
                                data.getDouble("wander_pt_" + i + "_x"),
                                data.getDouble("wander_pt_" + i + "_y"),
                                data.getDouble("wander_pt_" + i + "_z"));
                        notifyActivation(player, "§bТелепортация к точке " + (i + 1) + "!");
                    }
                    data.remove("wander_pt_" + i + "_x");
                    data.remove("wander_pt_" + i + "_y");
                    data.remove("wander_pt_" + i + "_z");
                    return;
                }
            }
            notifyActivation(player, "§cНет сохранённых точек!");
        }
    }

    // 2. Анализ биома — показывает биомы/структуры
    public static class BiomeAnalysis extends AbstractAbility {
        public BiomeAnalysis() {
            super(id("biome_analysis"), "ability.racecraft.biome_analysis.name",
                    "ability.racecraft.biome_analysis.desc", 600, 0, 12, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            var biome = level.getBiome(player.blockPosition()).value();
            player.sendSystemMessage(Component.literal("§bАнализ биома: §e" +
                    level.getBiome(player.blockPosition()).getRegisteredName() +
                    " §7[" + (int)player.getX() + " " + (int)player.getY() + " " + (int)player.getZ() + "]"));
            // Подсвечиваем мобов
            AABB box = player.getBoundingBox().inflate(50);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0)));
            notifyActivation(player, "§bАнализ биома выполнен!");
        }
    }

    // 3. Временная тропа — след и скорость (Ветка A)
    public static class TempTrail extends AbstractAbility {
        public TempTrail() {
            super(id("temp_trail"), "ability.racecraft.temp_trail.name",
                    "ability.racecraft.temp_trail.desc", 400, 20, 12, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0));
            notifyActivation(player, "§bВременная тропа — +30% скорость!");
        }
    }

    // 4. Портальная петля — два портала (Ветка A)
    public static class PortalLoop extends AbstractAbility {
        public PortalLoop() {
            super(id("portal_loop"), "ability.racecraft.portal_loop.name",
                    "ability.racecraft.portal_loop.desc", 600, 20, 12, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            // Сохраняем две точки для портала
            var data = player.getPersistentData();
            if (!data.contains("portal_a_x")) {
                data.putDouble("portal_a_x", player.getX());
                data.putDouble("portal_a_y", player.getY());
                data.putDouble("portal_a_z", player.getZ());
                notifyActivation(player, "§bПортал A создан. Используй снова для портала B.");
            } else {
                data.putDouble("portal_b_x", player.getX());
                data.putDouble("portal_b_y", player.getY());
                data.putDouble("portal_b_z", player.getZ());
                player.teleportTo(data.getDouble("portal_a_x"), data.getDouble("portal_a_y"), data.getDouble("portal_a_z"));
                data.remove("portal_a_x");
                notifyActivation(player, "§bПортальная петля — телепортация!");
            }
        }
    }

    // 5. Интуиция — подсветка сундуков и руд (Ветка B)
    public static class Intuition extends AbstractAbility {
        public Intuition() {
            super(id("intuition"), "ability.racecraft.intuition.name",
                    "ability.racecraft.intuition.desc", 600, 20, 12, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 300 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(30);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§bИнтуиция — скрытые объекты подсвечены!");
        }
    }

    // 6. Возврат — keepInventory при смерти (Ветка B)
    public static class Return extends AbstractAbility {
        public Return() {
            super(id("return"), "ability.racecraft.return.name",
                    "ability.racecraft.return.desc", 12000, 20, 12, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            player.getPersistentData().putBoolean("cartographer_keep_inv", true);
            player.getPersistentData().putLong("cartographer_keep_inv_expire", level.getGameTime() + 12000);
            notifyActivation(player, "§bВозврат активирован — сохранение инвентаря 10 мин!");
        }
    }

    // 7. Ориентация — всегда знает север (Tier 3)
    public static class Orientation extends AbstractAbility {
        public Orientation() {
            super(id("orientation"), "ability.racecraft.orientation.name",
                    "ability.racecraft.orientation.desc", 60, 30, 12, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            player.sendSystemMessage(Component.literal("§bПозиция: §e" + (int)player.getX() + " "
                    + (int)player.getY() + " " + (int)player.getZ() + " §7|§b Смотришь: §e" + player.getDirection().getName()));
            notifyActivation(player, "§bОриентация — ты знаешь, где находишься!");
        }
    }
}
