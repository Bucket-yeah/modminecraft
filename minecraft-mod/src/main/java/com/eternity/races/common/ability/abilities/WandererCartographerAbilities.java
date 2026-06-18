package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

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

    /** 12.1 Сохранение координат — до 5 точек (40t, 1 голод) */
    public static class SaveCoords extends AbstractAbility {
        public SaveCoords() {
            super(id("save_coords"), "ability.racecraft.save_coords.name",
                    "ability.racecraft.save_coords.desc", 40, 0, 12, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int maxSlots = 3 + getAccessoryLevel(player) / 3;
            var data = player.getPersistentData();
            for (int i = 0; i < maxSlots; i++) {
                if (!data.contains("wander_point_" + i + "_x")) {
                    data.putDouble("wander_point_" + i + "_x", player.getX());
                    data.putDouble("wander_point_" + i + "_y", player.getY());
                    data.putDouble("wander_point_" + i + "_z", player.getZ());
                    data.putString("wander_point_" + i + "_dim",
                            level.dimension().location().toString());
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§bТочка " + (i + 1) + " сохранена: §7"
                                + (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ()));
                    }
                    notifyActivation(player, "§bТочка " + (i + 1) + " сохранена!");
                    return;
                }
            }
            // Перезаписываем первую
            data.putDouble("wander_point_0_x", player.getX());
            data.putDouble("wander_point_0_y", player.getY());
            data.putDouble("wander_point_0_z", player.getZ());
            notifyActivation(player, "§bСлот 1 перезаписан!");
        }
    }

    /** 12.2 Телепорт к точке — 3 XP уровня (1200t, 3 XP) */
    public static class TeleportToPoint extends AbstractAbility {
        public TeleportToPoint() {
            super(id("teleport_to_point"), "ability.racecraft.teleport_to_point.name",
                    "ability.racecraft.teleport_to_point.desc", 1200, 0, 12, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 3)) return;
            var data = player.getPersistentData();
            // Телепортируем к последней сохранённой точке
            for (int i = 4; i >= 0; i--) {
                if (data.contains("wander_point_" + i + "_x")) {
                    double x = data.getDouble("wander_point_" + i + "_x");
                    double y = data.getDouble("wander_point_" + i + "_y");
                    double z = data.getDouble("wander_point_" + i + "_z");
                    player.teleportTo(x, y, z);
                    data.remove("wander_point_" + i + "_x");
                    data.remove("wander_point_" + i + "_y");
                    data.remove("wander_point_" + i + "_z");
                    notifyActivation(player, "§bТелепорт к точке " + (i + 1) + "! -3 XP");
                    return;
                }
            }
            notifyActivation(player, "§cНет сохранённых точек!");
        }
    }

    /** 12.3 Анализ биома — подробная информация о биоме (600t, 1 голод) */
    public static class BiomeAnalysis extends AbstractAbility {
        public BiomeAnalysis() {
            super(id("biome_analysis"), "ability.racecraft.biome_analysis.name",
                    "ability.racecraft.biome_analysis.desc", 600, 0, 12, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            var biomeHolder = level.getBiome(player.blockPosition());
            String biomeName = biomeHolder.unwrapKey()
                    .map(k -> k.location().getPath())
                    .orElse("unknown");
            int y = player.getBlockY();
            String dimension = level.dimension().location().getPath();
            String msg = "§b== Биом: §f" + biomeName + "§b ==\n§7Y=" + y + " Измерение: " + dimension;
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal(msg));
            }
            // Эффект — ночное зрение
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
            notifyActivation(player, "§bАнализ биома: " + biomeName);
        }
    }

    /** 12.4 Временный след — оставляет светящийся след на 15 сек (300t, 1 голод) */
    public static class TempTrail extends AbstractAbility {
        public TempTrail() {
            super(id("temp_trail"), "ability.racecraft.temp_trail.name",
                    "ability.racecraft.temp_trail.desc", 300, 20, 12, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0));
            player.getPersistentData().putLong("temp_trail_expire", level.getGameTime() + dur);
            notifyActivation(player, "§bСветящийся след на 15 сек!");
        }
    }

    /** 12.5 Портальная петля — телепорт к другой точке (2400t, 2 голода + 1 обсидиан) */
    public static class PortalLoop extends AbstractAbility {
        public PortalLoop() {
            super(id("portal_loop"), "ability.racecraft.portal_loop.name",
                    "ability.racecraft.portal_loop.desc", 2400, 20, 12, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            if (!hasItem(player, Items.OBSIDIAN, 1)) {
                notifyActivation(player, "§cНужен 1 обсидиан!");
                return;
            }
            removeItem(player, Items.OBSIDIAN, 1);
            // Телепорт на 100 блоков в направлении взгляда
            var look = player.getLookAngle();
            double dist = 100 * getDurationMultiplier(player);
            player.teleportTo(
                    player.getX() + look.x * dist,
                    player.getY(),
                    player.getZ() + look.z * dist);
            notifyActivation(player, "§bПортальная петля! Телепорт на 100 блоков вперёд.");
        }
    }

    /** 12.6 Интуиция — маркер враждебных мобов в 30 блоках (200t, 1 голод) */
    public static class Intuition extends AbstractAbility {
        public Intuition() {
            super(id("intuition"), "ability.racecraft.intuition.name",
                    "ability.racecraft.intuition.desc", 200, 20, 12, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = (30f + getAccessoryLevel(player) * 2f);
            int dur = (int)(200 * getDurationMultiplier(player));
            level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    player.getBoundingBox().inflate(radius))
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0)));
            notifyActivation(player, "§bИнтуиция! Враги подсвечены в 30 блоках.");
        }
    }

    /** 12.7 Возврат — 10-мин кулдаун, телепорт в точку спавна (12000t, ничего) */
    public static class Return extends AbstractAbility {
        public Return() {
            super(id("return"), "ability.racecraft.return.name",
                    "ability.racecraft.return.desc", 12000, 20, 12, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            boolean teleported = false;
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraft.core.BlockPos spawnPos = sp.getRespawnPosition();
                if (spawnPos != null) {
                    player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    notifyActivation(player, "§bВозврат к точке спавна!");
                    teleported = true;
                }
            }
            if (!teleported) {
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    var worldSpawn = sl.getSharedSpawnPos();
                    player.teleportTo(worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5);
                }
                notifyActivation(player, "§bВозврат к мировому спавну!");
            }
        }
    }

    /** 12.8 Ориентация — пассив: подсказки о направлении (0t, пассив) */
    public static class Orientation extends AbstractAbility {
        public Orientation() {
            super(id("orientation"), "ability.racecraft.orientation.name",
                    "ability.racecraft.orientation.desc", 0, 30, 12, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            int[] coords = {(int) player.getX(), (int) player.getY(), (int) player.getZ()};
            String dir = switch (player.getDirection()) {
                case NORTH -> "Север";
                case SOUTH -> "Юг";
                case EAST -> "Восток";
                case WEST -> "Запад";
                default -> "?";
            };
            notifyActivation(player, "§bОриентация: " + dir + " | §7" + coords[0] + ", " + coords[1] + ", " + coords[2]);
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
