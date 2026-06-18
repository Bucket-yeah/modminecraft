package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.mob.SummonedSpider;
import com.eternity.races.common.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

    // 0. Бросок кирки — дальняя атака
    public static class PickaxeThrow extends AbstractAbility {
        public PickaxeThrow() {
            super(id("pickaxe_throw"), "ability.racecraft.pickaxe_throw.name",
                    "ability.racecraft.pickaxe_throw.desc", 200, 0, 15, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            Vec3 look = player.getLookAngle();
            float dmg = 6f * getDamageMultiplier(player);
            // Атакуем моба в направлении взгляда (в пределах 15 блоков)
            AABB beam = player.getBoundingBox().expandTowards(look.scale(15));
            level.getEntitiesOfClass(LivingEntity.class, beam, e -> e != player).stream().findFirst()
                    .ifPresent(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    });
            notifyActivation(player, "§7Бросок кирки!");
        }
    }

    // 1. Создание туннеля — 3×3 туннель
    public static class TunnelDig extends AbstractAbility {
        public TunnelDig() {
            super(id("tunnel_dig"), "ability.racecraft.tunnel_dig.name",
                    "ability.racecraft.tunnel_dig.desc", 400, 0, 15, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            var dir = player.getDirection();
            var start = player.blockPosition();
            for (int d = 1; d <= 5; d++) {
                for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) {
                    var pos = start.relative(dir, d).offset(
                            dir.getAxis() == net.minecraft.core.Direction.Axis.X ? 0 : x,
                            y,
                            dir.getAxis() == net.minecraft.core.Direction.Axis.Z ? 0 : x);
                    if (!level.getBlockState(pos).is(net.minecraft.tags.BlockTags.WITHER_IMMUNE))
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
            notifyActivation(player, "§7Туннель 3×3 пробит!");
        }
    }

    // 2. Эхолокация — подсветка руд и сундуков
    public static class Echolocation extends AbstractAbility {
        public Echolocation() {
            super(id("gnome_echolocation"), "ability.racecraft.gnome_echolocation.name",
                    "ability.racecraft.gnome_echolocation.desc", 200, 0, 15, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(16);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§7Эхолокация — руды и мобы видны!");
        }
    }

    // 3. Каменная кожа — +6 брони (Ветка A)
    public static class StoneSkin extends AbstractAbility {
        public StoneSkin() {
            super(id("stone_skin"), "ability.racecraft.stone_skin.name",
                    "ability.racecraft.stone_skin.desc", 400, 20, 15, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 300 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 1));
            notifyActivation(player, "§7Каменная кожа! +6 брони, -40% скорость");
        }
    }

    // 4. Туннельный рывок — проход сквозь блоки (Ветка A)
    public static class TunnelDash extends AbstractAbility {
        public TunnelDash() {
            super(id("tunnel_dash"), "ability.racecraft.tunnel_dash.name",
                    "ability.racecraft.tunnel_dash.desc", 300, 20, 15, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            Vec3 dir = player.getLookAngle().normalize();
            double newX = player.getX() + dir.x * 5;
            double newY = player.getY() + dir.y * 3;
            double newZ = player.getZ() + dir.z * 5;
            player.teleportTo(newX, newY, newZ);
            notifyActivation(player, "§7Туннельный рывок!");
        }
    }

    // 5. Чутьё на опасность — видит мобов через стены (Ветка B)
    public static class DangerSense extends AbstractAbility {
        public DangerSense() {
            super(id("danger_sense"), "ability.racecraft.danger_sense.name",
                    "ability.racecraft.danger_sense.desc", 400, 20, 15, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 400 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(24);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§7Чутьё на опасность — мобы видны через стены!");
        }
    }

    // 6. Подземный союз — призыв пещерного паука (Ветка B)
    public static class UndergroundAlliance extends AbstractAbility {
        public UndergroundAlliance() {
            super(id("underground_alliance"), "ability.racecraft.underground_alliance.name",
                    "ability.racecraft.underground_alliance.desc", 800, 20, 15, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!level.isClientSide) {
                SummonedSpider spider = ModEntities.SUMMONED_SPIDER.get().create(level);
                if (spider != null) {
                    double ox = (level.random.nextDouble() - 0.5) * 3;
                    double oz = (level.random.nextDouble() - 0.5) * 3;
                    spider.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                    spider.setOwner(player);
                    spider.setLifetime(400); // 20 секунд
                    level.addFreshEntity(spider);
                }
            }
            notifyActivation(player, "§7Подземный союз — паук призван!");
        }
    }

    // 7. Сталагмит — каменный шип (Tier 3)
    public static class Stalagmite extends AbstractAbility {
        public Stalagmite() {
            super(id("stalagmite"), "ability.racecraft.stalagmite.name",
                    "ability.racecraft.stalagmite.desc", 300, 30, 15, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 5f * getDamageMultiplier(player);
            Vec3 look = player.getLookAngle();
            var target = player.blockPosition().relative(player.getDirection(), 4);
            // Поднимаем каменный столб
            for (int y = 0; y < 3; y++) {
                var pos = target.above(y);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.POINTED_DRIPSTONE.defaultBlockState());
                }
            }
            AABB box = new AABB(target).inflate(1);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
            notifyActivation(player, "§7Сталагмит! 5 урона");
        }
    }
}
