package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import com.eternity.races.common.mob.SummonedSlime;
import com.eternity.races.common.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 3 — Химера-Симбионт: все 8 способностей */
public class ChimeraSymbiontAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new PossessMob());
        reg.accept(new BlockPossession());
        reg.accept(new ParasiticTouch());
        reg.accept(new Colony());
        reg.accept(new Evolution());
        reg.accept(new Reproduction());
        reg.accept(new AliensForm());
        reg.accept(new Absorption());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 3.1 Вселение в моба — управление мобом (400t, 1 голод) */
    public static class PossessMob extends AbstractAbility {
        public PossessMob() {
            super(id("possess_mob"), "ability.racecraft.possess_mob.name",
                    "ability.racecraft.possess_mob.desc", 400, 0, 3, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            AABB box = player.getBoundingBox().inflate(6);
            var mobs = level.getEntitiesOfClass(Mob.class, box, e -> !player.is(e));
            if (mobs.isEmpty()) { notifyActivation(player, "§cНет подходящих мобов рядом!"); return; }
            Mob target = mobs.get(0);
            player.getPersistentData().putBoolean("chimera_possessing", true);
            player.getPersistentData().putUUID("chimera_possessed_mob", target.getUUID());
            player.getPersistentData().putLong("chimera_possess_expire", level.getGameTime() + 600);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 700, 0, false, false));
            player.setPos(target.getX(), target.getY(), target.getZ());
            notifyActivation(player, "§5Вселение в моба на 30 сек!");
        }
    }

    /** 3.2 Одержимость блоком — невидимость и неуязвимость 10 сек (300t, 1 голод) */
    public static class BlockPossession extends AbstractAbility {
        public BlockPossession() {
            super(id("block_possession"), "ability.racecraft.block_possession.name",
                    "ability.racecraft.block_possession.desc", 300, 0, 3, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            var pos = player.blockPosition().relative(player.getDirection());
            if (level.getBlockState(pos).isSolid()) {
                int dur = (int)(200 * getDurationMultiplier(player));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 127));
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
                notifyActivation(player, "§5Одержимость блоком — неуязвимость 10 сек!");
            } else {
                notifyActivation(player, "§cПрицелься на твёрдый блок!");
            }
        }
    }

    /** 3.3 Паразитическое касание — высасывание жизни 8 сек (400t, 1 голод) */
    public static class ParasiticTouch extends AbstractAbility {
        public ParasiticTouch() {
            super(id("parasitic_touch"), "ability.racecraft.parasitic_touch.name",
                    "ability.racecraft.parasitic_touch.desc", 400, 0, 3, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(160 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(3);
            var targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            if (targets.isEmpty()) { notifyActivation(player, "§cНет целей рядом!"); return; }
            targets.forEach(e -> {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, dur, 0));
            });
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 0));
            player.getPersistentData().putLong("parasitic_expire", level.getGameTime() + dur);
            notifyActivation(player, "§5Паразитическое касание — высасывание жизни!");
        }
    }

    /** 3.4 Колония — слизистый след, союзники +20% скорости 30 сек (900t, 2 голода) */
    public static class Colony extends AbstractAbility {
        public Colony() {
            super(id("colony"), "ability.racecraft.colony.name",
                    "ability.racecraft.colony.desc", 900, 20, 3, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            int dur = (int)(600 * getDurationMultiplier(player));
            player.getPersistentData().putLong("colony_expire", level.getGameTime() + dur);
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(Player.class, box)
                    .forEach(ally -> ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1)));
            notifyActivation(player, "§5Колония! Союзники ускорены на 30 сек.");
        }
    }

    /** 3.5 Эволюция — поглощение дропа моба (200t, дроп моба) */
    public static class Evolution extends AbstractAbility {
        public Evolution() {
            super(id("evolution"), "ability.racecraft.evolution.name",
                    "ability.racecraft.evolution.desc", 200, 20, 3, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            int dur = (int)(1200 * getDurationMultiplier(player));
            // Ищем моб-дроп в инвентаре
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.ROTTEN_FLESH)) {
                    stack.shrink(1);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 0));
                    notifyActivation(player, "§5Эволюция: Зомби → Сопротивление I!"); return;
                } else if (stack.is(Items.BONE)) {
                    stack.shrink(1);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 0));
                    notifyActivation(player, "§5Эволюция: Скелет → Сила I!"); return;
                } else if (stack.is(Items.GUNPOWDER)) {
                    stack.shrink(1);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 2));
                    notifyActivation(player, "§5Эволюция: Крипер → Иммунитет к взрывам!"); return;
                } else if (stack.is(Items.SPIDER_EYE) || stack.is(Items.STRING)) {
                    stack.shrink(1);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0));
                    notifyActivation(player, "§5Эволюция: Паук → Скорость I!"); return;
                }
            }
            notifyActivation(player, "§cНужен дроп моба (гниль/кость/порох/паутина)!");
        }
    }

    /** 3.6 Размножение — 3 слизня на 20 сек (1200t, 2 голода) */
    public static class Reproduction extends AbstractAbility {
        public Reproduction() {
            super(id("reproduction"), "ability.racecraft.reproduction.name",
                    "ability.racecraft.reproduction.desc", 1200, 20, 3, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 2)) return;
            if (!level.isClientSide) {
                int count = 3 + getAccessoryLevel(player) / 4;
                for (int i = 0; i < Math.min(count, 6); i++) {
                    SummonedSlime slime = ModEntities.SUMMONED_SLIME.get().create(level);
                    if (slime != null) {
                        double ox = (level.random.nextDouble() - 0.5) * 4;
                        double oz = (level.random.nextDouble() - 0.5) * 4;
                        slime.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                        slime.setOwner(player);
                        slime.setLifetime(400);
                        level.addFreshEntity(slime);
                    }
                }
            }
            notifyActivation(player, "§5Размножение — слизни призваны!");
        }
    }

    /** 3.7 Чужеродная форма — ×2 урон, -30% скорость, +10♥ на 12 сек (1600t, 3 голода) */
    public static class AliensForm extends AbstractAbility {
        public AliensForm() {
            super(id("aliens_form"), "ability.racecraft.aliens_form.name",
                    "ability.racecraft.aliens_form.desc", 1600, 20, 3, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            int dur = (int)(240 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 4));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, 4));
            player.getPersistentData().putLong("aliens_form_expire", level.getGameTime() + dur);
            notifyActivation(player, "§5Чужеродная форма! +100% урон, -30% скорость, +10♥.");
        }
    }

    /** 3.8 Поглощение — предмет → 4♥ лечение (100t, любой предмет) */
    public static class Absorption extends AbstractAbility {
        public Absorption() {
            super(id("absorption"), "ability.racecraft.absorption.name",
                    "ability.racecraft.absorption.desc", 100, 30, 3, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            int heal = 8 + (getAccessoryLevel(player) / 3) * 4;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                    player.heal(heal / 2f);
                    notifyActivation(player, "§5Поглощение! +" + (heal / 2) + "♥");
                    return;
                }
            }
            notifyActivation(player, "§cИнвентарь пуст!");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
