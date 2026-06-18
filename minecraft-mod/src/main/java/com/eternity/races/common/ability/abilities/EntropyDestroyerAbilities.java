package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Раса 8 — Энтропийный Разрушитель: все 8 способностей */
public class EntropyDestroyerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new Decay());
        reg.accept(new ChaosStrike());
        reg.accept(new EntropyExplosion());
        reg.accept(new Aging());
        reg.accept(new ArmorBreak());
        reg.accept(new BriefStability());
        reg.accept(new Annihilation());
        reg.accept(new MatterDecay());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Распад — мгновенное разрушение блока
    public static class Decay extends AbstractAbility {
        public Decay() {
            super(id("decay"), "ability.racecraft.decay.name",
                    "ability.racecraft.decay.desc", 100, 0, 8, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            var pos = player.blockPosition().relative(player.getDirection());
            if (!level.getBlockState(pos).is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
                level.destroyBlock(pos, true, player);
                notifyActivation(player, "§8Распад!");
            }
        }
    }

    // 1. Удар хаоса — урон + телепорт врага
    public static class ChaosStrike extends AbstractAbility {
        public ChaosStrike() {
            super(id("chaos_strike"), "ability.racecraft.chaos_strike.name",
                    "ability.racecraft.chaos_strike.desc", 300, 0, 8, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 10f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(4);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        if (Math.random() < 0.5) {
                            double rx = e.getX() + (Math.random() - 0.5) * 10;
                            double rz = e.getZ() + (Math.random() - 0.5) * 10;
                            if (e instanceof net.minecraft.server.level.ServerPlayer sp) {
                                sp.teleportTo(rx, e.getY(), rz);
                            } else {
                                e.teleportTo(rx, e.getY(), rz);
                            }
                        }
                    });
            notifyActivation(player, "§8Удар хаоса!");
        }
    }

    // 2. Энтропийный взрыв
    public static class EntropyExplosion extends AbstractAbility {
        public EntropyExplosion() {
            super(id("entropy_explosion"), "ability.racecraft.entropy_explosion.name",
                    "ability.racecraft.entropy_explosion.desc", 400, 0, 8, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 8f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(6);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.hurt(player.damageSources().playerAttack(player), dmg));
            player.hurt(player.damageSources().magic(), 4f);
            level.explode(player, player.getX(), player.getY(), player.getZ(), 2f,
                    Level.ExplosionInteraction.NONE);
            notifyActivation(player, "§8Энтропийный взрыв! -4♥ себе");
        }
    }

    // 3. Старение — постоянный урон (Ветка A)
    public static class Aging extends AbstractAbility {
        public Aging() {
            super(id("aging"), "ability.racecraft.aging.name",
                    "ability.racecraft.aging.desc", 400, 20, 8, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 300 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(6);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) dur, 1)));
            notifyActivation(player, "§8Старение — враги получают урон Иссушения!");
        }
    }

    // 4. Разрушение защиты — снятие брони (Ветка A)
    public static class ArmorBreak extends AbstractAbility {
        public ArmorBreak() {
            super(id("armor_break"), "ability.racecraft.armor_break.name",
                    "ability.racecraft.armor_break.desc", 400, 20, 8, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) dur, 3)));
            notifyActivation(player, "§8Разрушение защиты — броня ослаблена!");
        }
    }

    // 5. Кратковременная стабильность — временная стена (Ветка B)
    public static class BriefStability extends AbstractAbility {
        public BriefStability() {
            super(id("brief_stability"), "ability.racecraft.brief_stability.name",
                    "ability.racecraft.brief_stability.desc", 600, 20, 8, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Ставим стену из барьеров
            var dir = player.getDirection();
            var pos = player.blockPosition().relative(dir, 2);
            for (int y = 0; y < 3; y++) {
                level.setBlockAndUpdate(pos.above(y), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState());
            }
            player.getPersistentData().putLong("stability_wall_x", pos.getX());
            player.getPersistentData().putLong("stability_wall_y", pos.getY());
            player.getPersistentData().putLong("stability_wall_z", pos.getZ());
            player.getPersistentData().putLong("stability_expire", level.getGameTime() + 200);
            notifyActivation(player, "§8Кратковременная стабильность — стена создана!");
        }
    }

    // 6. Аннигиляция — удаление предмета врага (Ветка B)
    public static class Annihilation extends AbstractAbility {
        public Annihilation() {
            super(id("annihilation"), "ability.racecraft.annihilation.name",
                    "ability.racecraft.annihilation.desc", 600, 20, 8, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            float dmg = 5f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(4);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player).stream().findFirst()
                    .ifPresent(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        // Сбрасываем предмет в руке
                        if (!e.getMainHandItem().isEmpty()) {
                            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                                    level, e.getX(), e.getY(), e.getZ(), e.getMainHandItem().copy()));
                            e.getMainHandItem().setCount(0);
                        }
                    });
            notifyActivation(player, "§8Аннигиляция!");
        }
    }

    // 7. Распад материи — превращение блока в частицы регена (Tier 3)
    public static class MatterDecay extends AbstractAbility {
        public MatterDecay() {
            super(id("matter_decay"), "ability.racecraft.matter_decay.name",
                    "ability.racecraft.matter_decay.desc", 400, 30, 8, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            var pos = player.blockPosition().relative(player.getDirection());
            if (!level.getBlockState(pos).isAir()) {
                level.destroyBlock(pos, false, player);
                player.heal(4f);
                notifyActivation(player, "§8Распад материи! +2♥");
            }
        }
    }
}
