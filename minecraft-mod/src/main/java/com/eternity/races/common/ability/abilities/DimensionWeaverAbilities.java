package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    // 0. Сложение — обмен двух блоков
    public static class BlockSwap extends AbstractAbility {
        public BlockSwap() {
            super(id("block_swap"), "ability.racecraft.block_swap.name",
                    "ability.racecraft.block_swap.desc", 100, 0, 4, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            BlockPos pos1 = player.blockPosition().relative(player.getDirection(), 3);
            BlockPos pos2 = player.blockPosition().relative(player.getDirection(), 7);
            BlockState state1 = level.getBlockState(pos1);
            BlockState state2 = level.getBlockState(pos2);
            if (!state1.isAir() && !state2.isAir()) {
                level.setBlockAndUpdate(pos1, state2);
                level.setBlockAndUpdate(pos2, state1);
                notifyActivation(player, "§9Сложение — блоки обменяны!");
            }
        }
    }

    // 1. Разрыв — портальный коридор
    public static class Rift extends AbstractAbility {
        public Rift() {
            super(id("rift"), "ability.racecraft.rift.name",
                    "ability.racecraft.rift.desc", 400, 0, 4, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            Vec3 dir = player.getLookAngle();
            double x = player.getX() + dir.x * 5;
            double y = player.getY();
            double z = player.getZ() + dir.z * 5;
            player.teleportTo(x, y, z);
            notifyActivation(player, "§9Разрыв! Телепорт на 5 блоков.");
        }
    }

    // 2. Телепортационный якорь — метки возврата
    public static class TeleportAnchor extends AbstractAbility {
        public TeleportAnchor() {
            super(id("teleport_anchor"), "ability.racecraft.teleport_anchor.name",
                    "ability.racecraft.teleport_anchor.desc", 200, 0, 4, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            var data = player.getPersistentData();
            // Если якорь есть — телепортируемся, иначе ставим
            if (data.contains("anchor_x")) {
                player.teleportTo(data.getDouble("anchor_x"), data.getDouble("anchor_y"), data.getDouble("anchor_z"));
                data.remove("anchor_x"); data.remove("anchor_y"); data.remove("anchor_z");
                notifyActivation(player, "§9Возврат к якорю!");
            } else {
                data.putDouble("anchor_x", player.getX());
                data.putDouble("anchor_y", player.getY());
                data.putDouble("anchor_z", player.getZ());
                notifyActivation(player, "§9Якорь установлен.");
            }
        }
    }

    // 3. Сжатие — уменьшение дальности атаки врагов (Ветка A)
    public static class Compression extends AbstractAbility {
        public Compression() {
            super(id("compression"), "ability.racecraft.compression.name",
                    "ability.racecraft.compression.desc", 400, 20, 4, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 1)));
            notifyActivation(player, "§9Сжатие — дальность врагов уменьшена!");
        }
    }

    // 4. Изгиб — иммунитет к снарядам (Ветка A)
    public static class Bend extends AbstractAbility {
        public Bend() {
            super(id("bend"), "ability.racecraft.bend.name",
                    "ability.racecraft.bend.desc", 300, 20, 4, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 120 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 1));
            player.getPersistentData().putLong("bend_shield_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§9Изгиб — снаряды не опасны!");
        }
    }

    // 5. Карманное измерение — дополнительный инвентарь (Ветка B)
    public static class PocketDimension extends AbstractAbility {
        public PocketDimension() {
            super(id("pocket_dimension"), "ability.racecraft.pocket_dimension.name",
                    "ability.racecraft.pocket_dimension.desc", 2400, 20, 4, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Открываем эндер-сундук как заменитель карманного измерения
            if (level.isClientSide) return;
            var endChestPos = player.blockPosition().above(2);
            var endChestBlock = net.minecraft.world.level.block.Blocks.ENDER_CHEST.defaultBlockState();
            level.setBlockAndUpdate(endChestPos, endChestBlock);
            player.getPersistentData().putLong("pocket_dim_block_x", endChestPos.getX());
            player.getPersistentData().putLong("pocket_dim_block_y", endChestPos.getY());
            player.getPersistentData().putLong("pocket_dim_block_z", endChestPos.getZ());
            player.getPersistentData().putLong("pocket_dim_expire", level.getGameTime() + 2400);
            notifyActivation(player, "§9Карманное измерение открыто (2 мин)!");
        }
    }

    // 6. Пространственный взрыв — отбрасывание врагов (Ветка B)
    public static class SpatialBlast extends AbstractAbility {
        public SpatialBlast() {
            super(id("spatial_blast"), "ability.racecraft.spatial_blast.name",
                    "ability.racecraft.spatial_blast.desc", 400, 20, 4, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 3f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        Vec3 dir = e.position().subtract(player.position()).normalize();
                        e.push(dir.x * 2.5, 0.8, dir.z * 2.5);
                    });
            notifyActivation(player, "§9Пространственный взрыв!");
        }
    }

    // 7. Искривление пространства — снаряды меняют траекторию (Tier 3)
    public static class SpaceWarp extends AbstractAbility {
        public SpaceWarp() {
            super(id("space_warp"), "ability.racecraft.space_warp.name",
                    "ability.racecraft.space_warp.desc", 600, 30, 4, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 160 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 2));
            player.getPersistentData().putLong("space_warp_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§9Искривление пространства активно!");
        }
    }
}
