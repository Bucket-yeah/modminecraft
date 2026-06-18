package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 4 — Ткач Измерений: все 8 способностей */
public class DimensionWeaverAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new BlockSwap());
        reg.accept(new Rift());
        reg.accept(new TeleportAnchor());
        reg.accept(new Compression());
        reg.accept(new Bend());
        reg.accept(new PocketDimension());
        reg.accept(new SpatialBlast());
        reg.accept(new SpaceWarp());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 4.1 Сложение — обмен двух блоков в 10 блоках (200t, 1 голод) */
    public static class BlockSwap extends AbstractAbility {
        public BlockSwap() {
            super(id("block_swap"), "ability.racecraft.block_swap.name",
                    "ability.racecraft.block_swap.desc", 200, 0, 4, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            BlockPos pos1 = player.blockPosition().relative(player.getDirection(), 3);
            BlockPos pos2 = player.blockPosition().relative(player.getDirection(), 7);
            BlockState state1 = level.getBlockState(pos1);
            BlockState state2 = level.getBlockState(pos2);
            if (!state1.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
                    && !state2.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
                    && !state1.isAir() && !state2.isAir()) {
                level.setBlockAndUpdate(pos1, state2);
                level.setBlockAndUpdate(pos2, state1);
                notifyActivation(player, "§9Сложение — блоки обменяны!");
            } else {
                notifyActivation(player, "§cНет двух твёрдых блоков перед тобой!");
            }
        }
    }

    /** 4.2 Разрыв — телепорт на 5 блоков вперёд (400t, 2 голода) */
    public static class Rift extends AbstractAbility {
        public Rift() {
            super(id("rift"), "ability.racecraft.rift.name",
                    "ability.racecraft.rift.desc", 400, 0, 4, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            // 10% шанс телепорта на 10 блоков вверх (штраф расы)
            if (level.random.nextDouble() < 0.10) {
                player.teleportTo(player.getX(), player.getY() + 10, player.getZ());
                notifyActivation(player, "§cОшибка разрыва — телепорт вверх!");
                return;
            }
            Vec3 dir = player.getLookAngle();
            double len = 5 + getDurationMultiplier(player);
            double x = player.getX() + dir.x * len;
            double y = player.getY();
            double z = player.getZ() + dir.z * len;
            player.teleportTo(x, y, z);
            notifyActivation(player, "§9Разрыв!");
        }
    }

    /** 4.3 Телепортационный якорь — до 3 маркеров, телепорт -3♥ (100t/600t) */
    public static class TeleportAnchor extends AbstractAbility {
        public TeleportAnchor() {
            super(id("teleport_anchor"), "ability.racecraft.teleport_anchor.name",
                    "ability.racecraft.teleport_anchor.desc", 100, 0, 4, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            var data = player.getPersistentData();
            int maxAnchors = 3 + getAccessoryLevel(player) / 3;
            // Ищем ближайший якорь для телепортации
            for (int i = 0; i < maxAnchors; i++) {
                if (data.contains("anchor_" + i + "_x")) {
                    double ax = data.getDouble("anchor_" + i + "_x");
                    double ay = data.getDouble("anchor_" + i + "_y");
                    double az = data.getDouble("anchor_" + i + "_z");
                    double dist = player.distanceToSqr(ax, ay, az);
                    if (dist < 16) {  // игрок рядом — телепортируемся
                        if (player.getHealth() <= 3f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
                        player.hurt(player.damageSources().magic(), 3f);
                        player.teleportTo(ax, ay, az);
                        data.remove("anchor_" + i + "_x");
                        data.remove("anchor_" + i + "_y");
                        data.remove("anchor_" + i + "_z");
                        notifyActivation(player, "§9Телепорт к якорю! -3♥");
                        return;
                    }
                }
            }
            // Ставим новый якорь
            for (int i = 0; i < maxAnchors; i++) {
                if (!data.contains("anchor_" + i + "_x")) {
                    data.putDouble("anchor_" + i + "_x", player.getX());
                    data.putDouble("anchor_" + i + "_y", player.getY());
                    data.putDouble("anchor_" + i + "_z", player.getZ());
                    notifyActivation(player, "§9Якорь " + (i + 1) + " установлен.");
                    return;
                }
            }
            notifyActivation(player, "§cВсе якоря заняты!");
        }
    }

    /** 4.4 Сжатие — -50% дальность атаки врагов 10 сек (600t, 1 голод) */
    public static class Compression extends AbstractAbility {
        public Compression() {
            super(id("compression"), "ability.racecraft.compression.name",
                    "ability.racecraft.compression.desc", 600, 20, 4, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 10f * (1f + getAccessoryLevel(player) * 0.05f);
            int dur = (int)(200 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 1)));
            notifyActivation(player, "§9Сжатие — дальность врагов уменьшена на 10 сек!");
        }
    }

    /** 4.5 Изгиб — невидимость для снарядов 6 сек (500t, 1 голод) */
    public static class Bend extends AbstractAbility {
        public Bend() {
            super(id("bend"), "ability.racecraft.bend.name",
                    "ability.racecraft.bend.desc", 500, 20, 4, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(120 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.getPersistentData().putLong("bend_shield_expire", level.getGameTime() + dur);
            notifyActivation(player, "§9Изгиб — невидим для снарядов 6 сек!");
        }
    }

    /** 4.6 Карманное измерение — 9-слотный инвентарь 2 мин (2400t, 1 XP уровень) */
    public static class PocketDimension extends AbstractAbility {
        public PocketDimension() {
            super(id("pocket_dimension"), "ability.racecraft.pocket_dimension.name",
                    "ability.racecraft.pocket_dimension.desc", 2400, 20, 4, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 1)) return;
            if (level.isClientSide) return;
            // Открываем эндер-сундук как альтернативу карманному измерению
            var pos = player.blockPosition().above(2);
            while (!level.getBlockState(pos).isAir() && pos.getY() < 320) pos = pos.above();
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.ENDER_CHEST.defaultBlockState());
            player.getPersistentData().putLong("pocket_dim_x", pos.getX());
            player.getPersistentData().putLong("pocket_dim_y", pos.getY());
            player.getPersistentData().putLong("pocket_dim_z", pos.getZ());
            player.getPersistentData().putLong("pocket_dim_expire", level.getGameTime() + 2400);
            notifyActivation(player, "§9Карманное измерение открыто на 2 мин!");
        }
    }

    /** 4.7 Пространственный взрыв — отбрасывание на 20 блоков, 3 урона (1200t, 3 XP) */
    public static class SpatialBlast extends AbstractAbility {
        public SpatialBlast() {
            super(id("spatial_blast"), "ability.racecraft.spatial_blast.name",
                    "ability.racecraft.spatial_blast.desc", 1200, 20, 4, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 3)) return;
            float radius = 15f * (1f + getAccessoryLevel(player) * 0.05f);
            float dmg = 3f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        Vec3 dir = e.position().subtract(player.position()).normalize();
                        e.push(dir.x * 3, 0.8, dir.z * 3);
                    });
            notifyActivation(player, "§9Пространственный взрыв!");
        }
    }

    /** 4.8 Искривление пространства — снаряды по случайным траекториям 8 сек (900t, 2 голода) */
    public static class SpaceWarp extends AbstractAbility {
        public SpaceWarp() {
            super(id("space_warp"), "ability.racecraft.space_warp.name",
                    "ability.racecraft.space_warp.desc", 900, 30, 4, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int dur = (int)(160 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 2));
            player.getPersistentData().putLong("space_warp_expire", level.getGameTime() + dur);
            notifyActivation(player, "§9Искривление пространства! Снаряды отклоняются 8 сек.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
