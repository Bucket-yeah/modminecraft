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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    // 0. Вселение в моба
    public static class PossessMob extends AbstractAbility {
        public PossessMob() {
            super(id("possess_mob"), "ability.racecraft.possess_mob.name",
                    "ability.racecraft.possess_mob.desc", 1200, 0, 3, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(5);
            var mobs = level.getEntitiesOfClass(Mob.class, box, e -> e != player && !(e instanceof Monster m && m.getTarget() == player));
            if (mobs.isEmpty()) {
                notifyActivation(player, "§cНет подходящих мобов рядом!");
                return;
            }
            Mob target = mobs.get(0);
            player.getPersistentData().putBoolean("chimera_possessing", true);
            player.getPersistentData().putUUID("chimera_possessed_mob", target.getUUID());
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
            player.setPos(target.getX(), target.getY(), target.getZ());
            notifyActivation(player, "§5Вселение выполнено!");
        }
    }

    // 1. Одержимость блоком — слияние с блоком
    public static class BlockPossession extends AbstractAbility {
        public BlockPossession() {
            super(id("block_possession"), "ability.racecraft.block_possession.name",
                    "ability.racecraft.block_possession.desc", 400, 0, 3, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 4));
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) dur, 0));
            notifyActivation(player, "§5Одержимость блоком — неуязвимость!");
        }
    }

    // 2. Паразитическое касание — высасывание жизни
    public static class ParasiticTouch extends AbstractAbility {
        public ParasiticTouch() {
            super(id("parasitic_touch"), "ability.racecraft.parasitic_touch.name",
                    "ability.racecraft.parasitic_touch.desc", 200, 0, 3, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(3);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), 2f);
                        player.heal(1f);
                    });
            notifyActivation(player, "§5Паразитическое касание!");
        }
    }

    // 3. Колония — слизистый след (Ветка A)
    public static class Colony extends AbstractAbility {
        public Colony() {
            super(id("colony"), "ability.racecraft.colony.name",
                    "ability.racecraft.colony.desc", 300, 20, 3, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            // Скорость союзникам в радиусе
            AABB box = player.getBoundingBox().inflate(8);
            level.getEntitiesOfClass(Player.class, box)
                    .forEach(ally -> ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) dur, 1)));
            notifyActivation(player, "§5Колония! Союзники ускорены.");
        }
    }

    // 4. Эволюция — поглощение дропа (Ветка A)
    public static class Evolution extends AbstractAbility {
        public Evolution() {
            super(id("evolution"), "ability.racecraft.evolution.name",
                    "ability.racecraft.evolution.desc", 600, 20, 3, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 1200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) dur, 0));
            notifyActivation(player, "§5Эволюция! Временные черты получены.");
        }
    }

    // 5. Размножение — призыв 3 слизней (Ветка B)
    public static class Reproduction extends AbstractAbility {
        public Reproduction() {
            super(id("reproduction"), "ability.racecraft.reproduction.name",
                    "ability.racecraft.reproduction.desc", 800, 20, 3, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!level.isClientSide) {
                for (int i = 0; i < 3; i++) {
                    SummonedSlime slime = ModEntities.SUMMONED_SLIME.get().create(level);
                    if (slime != null) {
                        double ox = (level.random.nextDouble() - 0.5) * 4;
                        double oz = (level.random.nextDouble() - 0.5) * 4;
                        slime.setPos(player.getX() + ox, player.getY(), player.getZ() + oz);
                        slime.setOwner(player);
                        slime.setLifetime(400); // 20 секунд
                        level.addFreshEntity(slime);
                    }
                }
            }
            notifyActivation(player, "§5Размножение — слизни призваны!");
        }
    }

    // 6. Чужеродная форма — увеличение размера и урона (Ветка B)
    public static class AliensForm extends AbstractAbility {
        public AliensForm() {
            super(id("aliens_form"), "ability.racecraft.aliens_form.name",
                    "ability.racecraft.aliens_form.desc", 600, 20, 3, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 100 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) dur, 4));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) dur, 1));
            notifyActivation(player, "§5Чужеродная форма! Огромная сила, но медлительность.");
        }
    }

    // 7. Поглощение — поедание предмета, восстановление 4♥ (Tier 3)
    public static class Absorption extends AbstractAbility {
        public Absorption() {
            super(id("absorption"), "ability.racecraft.absorption.name",
                    "ability.racecraft.absorption.desc", 200, 30, 3, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            // Потребляем любой предмет из инвентаря
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.AIR)) {
                    stack.shrink(1);
                    player.heal(8f);
                    notifyActivation(player, "§5Поглощение! +4♥");
                    return;
                }
            }
            notifyActivation(player, "§cИнвентарь пуст!");
        }
    }
}
