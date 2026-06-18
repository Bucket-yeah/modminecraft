package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 11 — Садовник-Симбиот: все 8 способностей */
public class GardenerSymbiontAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new GrowthBlessing());
        reg.accept(new HealingJuice());
        reg.accept(new RootBond());
        reg.accept(new NatureArmor());
        reg.accept(new Photosynthesis());
        reg.accept(new MetabolismGardener());
        reg.accept(new FlowerRain());
        reg.accept(new Entangle());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Благословение роста — мгновенный рост урожая
    public static class GrowthBlessing extends AbstractAbility {
        public GrowthBlessing() {
            super(id("growth_blessing"), "ability.racecraft.growth_blessing.name",
                    "ability.racecraft.growth_blessing.desc", 400, 0, 11, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            var center = player.blockPosition();
            int grown = 0;
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                var pos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop) {
                    if (!crop.isMaxAge(state)) {
                        level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getMaxAge()));
                        grown++;
                    }
                }
            }
            notifyActivation(player, "§aБлагословение роста! Выросло: " + grown + " культур.");
        }
    }

    // 1. Целебный сок — лечение при касании травы/листьев
    public static class HealingJuice extends AbstractAbility {
        public HealingJuice() {
            super(id("healing_juice"), "ability.racecraft.healing_juice.name",
                    "ability.racecraft.healing_juice.desc", 300, 0, 11, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            var pos = player.blockPosition();
            var block = level.getBlockState(pos).getBlock();
            var below = level.getBlockState(pos.below()).getBlock();
            if (block instanceof LeavesBlock || below instanceof GrassBlock || below == net.minecraft.world.level.block.Blocks.DIRT) {
                player.heal(4f);
                // Лечим союзников рядом
                AABB box = player.getBoundingBox().inflate(5);
                level.getEntitiesOfClass(Player.class, box)
                        .forEach(ally -> ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1)));
                notifyActivation(player, "§aЦелебный сок — лечение!");
            } else {
                notifyActivation(player, "§cВстань на траву или под листья!");
            }
        }
    }

    // 2. Корневая связь — усиление роста в зоне
    public static class RootBond extends AbstractAbility {
        public RootBond() {
            super(id("root_bond"), "ability.racecraft.root_bond.name",
                    "ability.racecraft.root_bond.desc", 600, 0, 11, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            var center = player.blockPosition();
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                var pos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                    // Боунмил эффект
                    level.setBlockAndUpdate(pos, crop.getStateForAge(
                            Math.min(crop.getMaxAge(), crop.getAge(state) + 2)));
                }
            }
            notifyActivation(player, "§aКорневая связь — ускоренный рост!");
        }
    }

    // 3. Природная броня — листовая броня (Ветка A)
    public static class NatureArmor extends AbstractAbility {
        public NatureArmor() {
            super(id("nature_armor"), "ability.racecraft.nature_armor.name",
                    "ability.racecraft.nature_armor.desc", 400, 20, 11, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 400 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 1));
            // Urон отражается вручную в RaceEventHandler — здесь помечаем как активный
            player.getPersistentData().putLong("nature_armor_expire", player.level().getGameTime() + (long) dur);
            notifyActivation(player, "§aПриродная броня +4 брони!");
        }
    }

    // 4. Фотосинтез — реген на солнце (Ветка A)
    public static class Photosynthesis extends AbstractAbility {
        public Photosynthesis() {
            super(id("photosynthesis"), "ability.racecraft.photosynthesis.name",
                    "ability.racecraft.photosynthesis.desc", 600, 20, 11, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (level.isDay() && level.canSeeSky(player.blockPosition())) {
                float dur = 400 * getDurationMultiplier(player);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) dur, 1));
                player.getFoodData().eat(6, 1f);
                notifyActivation(player, "§aФотосинтез — реген и еда!");
            } else {
                notifyActivation(player, "§cНужен солнечный свет!");
            }
        }
    }

    // 5. Обмен веществ — мясо → костная мука (Ветка B)
    public static class MetabolismGardener extends AbstractAbility {
        public MetabolismGardener() {
            super(id("metabolism_gardener"), "ability.racecraft.metabolism_gardener.name",
                    "ability.racecraft.metabolism_gardener.desc", 60, 20, 11, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            int converted = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(net.minecraft.world.item.Items.ROTTEN_FLESH) ||
                        stack.is(net.minecraft.world.item.Items.BEEF) ||
                        stack.is(net.minecraft.world.item.Items.PORKCHOP)) {
                    int count = stack.getCount();
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.addItem(new ItemStack(Items.BONE_MEAL, count));
                    converted += count;
                }
            }
            if (converted > 0) {
                notifyActivation(player, "§aОбмен веществ: " + converted + " мяса → костная мука!");
            } else {
                notifyActivation(player, "§cМяса не найдено в инвентаре.");
            }
        }
    }

    // 6. Цветочный дождь — дождь из цветов, рост (Ветка B)
    public static class FlowerRain extends AbstractAbility {
        public FlowerRain() {
            super(id("flower_rain"), "ability.racecraft.flower_rain.name",
                    "ability.racecraft.flower_rain.desc", 800, 20, 11, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            GrowthBlessing gb = new GrowthBlessing();
            gb.execute(player, level);
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 400, 1));
            notifyActivation(player, "§aЦветочный дождь — мгновенный рост!");
        }
    }

    // 7. Оплетение — замедление и урон врагам (Tier 3)
    public static class Entangle extends AbstractAbility {
        public Entangle() {
            super(id("entangle"), "ability.racecraft.entangle.name",
                    "ability.racecraft.entangle.desc", 600, 30, 11, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            float dmg = 2f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 3));
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                    });
            notifyActivation(player, "§aОплетение — враги опутаны!");
        }
    }
}
