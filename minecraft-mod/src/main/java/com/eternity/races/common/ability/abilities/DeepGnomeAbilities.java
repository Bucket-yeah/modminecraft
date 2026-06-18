package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.mob.SummonedSpider;
import com.eternity.races.common.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 15 — Глубинный Гном: все 8 способностей */
public class DeepGnomeAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new PickaxeThrow());
        reg.accept(new TunnelDig());
        reg.accept(new Echolocation());
        reg.accept(new StoneSkin());
        reg.accept(new TunnelDash());
        reg.accept(new DangerSense());
        reg.accept(new UndergroundAlliance());
        reg.accept(new Stalagmite());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 15.1 Бросок кирки — метает кирку, 6 урона (100t, 1 голод) */
    public static class PickaxeThrow extends AbstractAbility {
        public PickaxeThrow() {
            super(id("pickaxe_throw"), "ability.racecraft.pickaxe_throw.name",
                    "ability.racecraft.pickaxe_throw.desc", 100, 0, 15, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float dmg = 6f * getDamageMultiplier(player);
            Vec3 look = player.getLookAngle();
            // Атакуем первого моба по лучу взгляда
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(look.scale(20));
            AABB scanBox = new AABB(
                    Math.min(start.x, end.x) - 0.5, Math.min(start.y, end.y) - 0.5, Math.min(start.z, end.z) - 0.5,
                    Math.max(start.x, end.x) + 0.5, Math.max(start.y, end.y) + 0.5, Math.max(start.z, end.z) + 0.5);
            boolean hit = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, scanBox, en -> !player.is(en))) {
                e.hurt(player.damageSources().playerAttack(player), dmg);
                hit = true;
                break;
            }
            if (hit) notifyActivation(player, "§7Бросок кирки! Урон: " + (int) dmg);
            else notifyActivation(player, "§cНет целей в прицеле!");
        }
    }

    /** 15.2 Туннельное рытьё — 5×1 туннель (400t, 3 голода) */
    public static class TunnelDig extends AbstractAbility {
        public TunnelDig() {
            super(id("tunnel_dig"), "ability.racecraft.tunnel_dig.name",
                    "ability.racecraft.tunnel_dig.desc", 400, 0, 15, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            if (level.isClientSide) return;
            int depth = 5 + getAccessoryLevel(player) / 2;
            var dir = player.getDirection();
            BlockPos start = player.blockPosition();
            for (int i = 1; i <= depth; i++) {
                BlockPos pos = start.relative(dir, i);
                for (int dy = 0; dy < 2; dy++) {
                    BlockPos tp = pos.above(dy);
                    var state = level.getBlockState(tp);
                    if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
                        level.destroyBlock(tp, true, player);
                    }
                }
            }
            notifyActivation(player, "§7Туннельное рытьё! " + depth + " блоков вперёд.");
        }
    }

    /** 15.3 Эхолокация — подсветка существ и блоков в 20 блоках (200t, 1 голод) */
    public static class Echolocation extends AbstractAbility {
        public Echolocation() {
            super(id("gnome_echolocation"), "ability.racecraft.gnome_echolocation.name",
                    "ability.racecraft.gnome_echolocation.desc", 200, 0, 15, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = (20f + getAccessoryLevel(player) * 2f);
            int dur = (int)(200 * getDurationMultiplier(player));
            level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0)));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur, 0, false, false));
            notifyActivation(player, "§7Эхолокация! Существа подсвечены в 20 блоках.");
        }
    }

    /** 15.4 Каменная кожа — защита от урона камня 15 сек (600t, 1 голод) */
    public static class StoneSkin extends AbstractAbility {
        public StoneSkin() {
            super(id("stone_skin"), "ability.racecraft.stone_skin.name",
                    "ability.racecraft.stone_skin.desc", 600, 20, 15, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(300 * getDurationMultiplier(player));
            int lvl = getAccessoryLevel(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1 + lvl / 5));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
            player.getPersistentData().putLong("stone_skin_expire", level.getGameTime() + dur);
            notifyActivation(player, "§7Каменная кожа! +защита 15 сек.");
        }
    }

    /** 15.5 Туннельный рывок — рывок 5 блоков -2♥ (200t, 2♥) */
    public static class TunnelDash extends AbstractAbility {
        public TunnelDash() {
            super(id("tunnel_dash"), "ability.racecraft.tunnel_dash.name",
                    "ability.racecraft.tunnel_dash.desc", 200, 20, 15, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (player.getHealth() <= 2f) { notifyActivation(player, "§cНедостаточно здоровья!"); return; }
            player.hurt(player.damageSources().magic(), 2f);
            Vec3 look = player.getLookAngle().normalize();
            double dist = 5 + getAccessoryLevel(player) * 0.3;
            player.push(look.x * dist * 0.3, look.y * 0.3, look.z * dist * 0.3);
            player.teleportTo(
                    player.getX() + look.x * dist,
                    player.getY(),
                    player.getZ() + look.z * dist);
            notifyActivation(player, "§7Туннельный рывок! -2♥");
        }
    }

    /** 15.6 Чувство опасности — пассив: предупреждение о мобах (0t, пассив) */
    public static class DangerSense extends AbstractAbility {
        public DangerSense() {
            super(id("danger_sense"), "ability.racecraft.danger_sense.name",
                    "ability.racecraft.danger_sense.desc", 0, 20, 15, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Пассивный — проверяем ближайших врагов прямо сейчас
            int radius = 16 + getAccessoryLevel(player) * 2;
            long count = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                    player.getBoundingBox().inflate(radius)).size();
            if (count > 0) {
                notifyActivation(player, "§c⚠ Чувство опасности! " + count + " врагов в " + radius + " блоках!");
            } else {
                notifyActivation(player, "§7Чувство опасности: врагов нет.");
            }
        }
    }

    /** 15.7 Подземный союз — призыв 3 пауков (1800t, 2 голода) */
    public static class UndergroundAlliance extends AbstractAbility {
        public UndergroundAlliance() {
            super(id("underground_alliance"), "ability.racecraft.underground_alliance.name",
                    "ability.racecraft.underground_alliance.desc", 1800, 20, 15, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            if (!level.isClientSide) {
                int count = 3 + getAccessoryLevel(player) / 3;
                for (int i = 0; i < Math.min(count, 6); i++) {
                    SummonedSpider spider = ModEntities.SUMMONED_SPIDER.get().create(level);
                    if (spider != null) {
                        double ox = (level.random.nextDouble() - 0.5) * 4;
                        double oz = (level.random.nextDouble() - 0.5) * 4;
                        spider.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                        spider.setOwner(player);
                        spider.setLifetime(1800);
                        level.addFreshEntity(spider);
                    }
                }
            }
            notifyActivation(player, "§7Подземный союз! " + (3 + getAccessoryLevel(player) / 3) + " паука призвано.");
        }
    }

    /** 15.8 Сталагмит — создание острых каменных пиков (300t, 1 голод) */
    public static class Stalagmite extends AbstractAbility {
        public Stalagmite() {
            super(id("stalagmite"), "ability.racecraft.stalagmite.name",
                    "ability.racecraft.stalagmite.desc", 300, 30, 15, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            if (level.isClientSide) return;
            float dmg = 4f * getDamageMultiplier(player);
            int count = 5 + getAccessoryLevel(player) / 2;
            // Создаём сталагмиты в радиусе 6 блоков
            for (int i = 0; i < count; i++) {
                double ox = (level.random.nextDouble() - 0.5) * 12;
                double oz = (level.random.nextDouble() - 0.5) * 12;
                BlockPos pos = player.blockPosition().offset((int) ox, 0, (int) oz);
                // Находим поверхность
                while (!level.getBlockState(pos).isAir() && pos.getY() < 320) pos = pos.above();
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                    level.setBlockAndUpdate(pos, Blocks.POINTED_DRIPSTONE.defaultBlockState());
                    // Урон существам на позиции
                    level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(pos).inflate(0.5), e -> e != player)
                            .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
                }
            }
            notifyActivation(player, "§7Сталагмит! " + count + " пиков вокруг.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
