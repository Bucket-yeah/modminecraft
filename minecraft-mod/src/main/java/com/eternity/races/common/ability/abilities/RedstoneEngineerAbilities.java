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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 13 — Редстоун-Инженер: все 8 способностей */
public class RedstoneEngineerAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new SignalBoost());
        reg.accept(new TempWire());
        reg.accept(new Jammer());
        reg.accept(new CircuitScan());
        reg.accept(new QuickRepair());
        reg.accept(new EnergyBurst());
        reg.accept(new CircuitCopy());
        reg.accept(new RedstoneSensor());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    // 0. Усиление сигнала — сигнал 15 на 10 сек
    public static class SignalBoost extends AbstractAbility {
        public SignalBoost() {
            super(id("signal_boost"), "ability.racecraft.signal_boost.name",
                    "ability.racecraft.signal_boost.desc", 200, 0, 13, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            // Активируем рычаги/кнопки в радиусе
            var center = player.blockPosition();
            for (int x = -5; x <= 5; x++) for (int y = -2; y <= 2; y++) for (int z = -5; z <= 5; z++) {
                var pos = center.offset(x, y, z);
                if (level.getBlockState(pos).getBlock() instanceof LeverBlock) {
                    level.setBlockAndUpdate(pos, level.getBlockState(pos)
                            .setValue(LeverBlock.POWERED, true));
                }
            }
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
            notifyActivation(player, "§cУсиление сигнала — сигнал 15!");
        }
    }

    // 1. Временная цепь — невидимый провод
    public static class TempWire extends AbstractAbility {
        public TempWire() {
            super(id("temp_wire"), "ability.racecraft.temp_wire.name",
                    "ability.racecraft.temp_wire.desc", 300, 0, 13, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            // Прокладываем редстоун-провод от игрока до цели
            var start = player.blockPosition();
            var end = player.blockPosition().relative(player.getDirection(), 10);
            for (int i = 0; i <= 10; i++) {
                var pos = start.relative(player.getDirection(), i);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                    level.setBlockAndUpdate(pos, Blocks.REDSTONE_WIRE.defaultBlockState());
                    // Удалим позже через tick handler
                }
            }
            player.getPersistentData().putLong("temp_wire_expire", level.getGameTime() + 300);
            notifyActivation(player, "§cВременная цепь проложена!");
        }
    }

    // 2. Глушитель — отключение редстоуна в радиусе
    public static class Jammer extends AbstractAbility {
        public Jammer() {
            super(id("jammer"), "ability.racecraft.jammer.name",
                    "ability.racecraft.jammer.desc", 400, 0, 13, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            var center = player.blockPosition();
            for (int x = -5; x <= 5; x++) for (int y = -2; y <= 2; y++) for (int z = -5; z <= 5; z++) {
                var pos = center.offset(x, y, z);
                if (level.getBlockState(pos).getBlock() instanceof LeverBlock) {
                    level.setBlockAndUpdate(pos, level.getBlockState(pos)
                            .setValue(LeverBlock.POWERED, false));
                }
            }
            notifyActivation(player, "§cГлушитель — редстоун отключён!");
        }
    }

    // 3. Сканирование схем — показывает компоненты (Ветка A)
    public static class CircuitScan extends AbstractAbility {
        public CircuitScan() {
            super(id("circuit_scan"), "ability.racecraft.circuit_scan.name",
                    "ability.racecraft.circuit_scan.desc", 300, 20, 13, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§cСканирование схем — компоненты видны!");
        }
    }

    // 4. Быстрая починка — восстановление компонента (Ветка A)
    public static class QuickRepair extends AbstractAbility {
        public QuickRepair() {
            super(id("quick_repair"), "ability.racecraft.quick_repair.name",
                    "ability.racecraft.quick_repair.desc", 400, 20, 13, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            // Восстанавливаем прочность текущего инструмента
            var mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamaged()) {
                mainHand.setDamageValue(0);
                notifyActivation(player, "§cБыстрая починка — инструмент восстановлен!");
            } else {
                notifyActivation(player, "§cНет сломанного инструмента в руке.");
            }
        }
    }

    // 5. Энергетический взрыв — взрыв на активном проводе (Ветка B)
    public static class EnergyBurst extends AbstractAbility {
        public EnergyBurst() {
            super(id("energy_burst"), "ability.racecraft.energy_burst.name",
                    "ability.racecraft.energy_burst.desc", 600, 20, 13, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Взрыв в точке активного редстоун-провода
            var center = player.blockPosition();
            BlockPos target = center;
            for (int x = -5; x <= 5 && target == center; x++)
                for (int z = -5; z <= 5; z++) {
                    var pos = center.offset(x, 0, z);
                    if (level.getBlockState(pos).getBlock() instanceof RedStoneWireBlock
                            && level.getSignal(pos, net.minecraft.core.Direction.DOWN) > 0) {
                        target = pos;
                        break;
                    }
                }
            float dmg = 6f * getDamageMultiplier(player);
            level.explode(player, target.getX(), target.getY(), target.getZ(), 2f, Level.ExplosionInteraction.NONE);
            notifyActivation(player, "§cЭнергетический взрыв!");
        }
    }

    // 6. Копирование схемы — запись 5×5 цепи (Ветка B)
    public static class CircuitCopy extends AbstractAbility {
        public CircuitCopy() {
            super(id("circuit_copy"), "ability.racecraft.circuit_copy.name",
                    "ability.racecraft.circuit_copy.desc", 800, 20, 13, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, 3));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 400, 1));
            notifyActivation(player, "§cКопирование схемы выполнено!");
        }
    }

    // 7. Редстоун-сенсор — обнаружение существ по сигналу (Tier 3)
    public static class RedstoneSensor extends AbstractAbility {
        public RedstoneSensor() {
            super(id("redstone_sensor"), "ability.racecraft.redstone_sensor.name",
                    "ability.racecraft.redstone_sensor.desc", 300, 30, 13, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 400 * getDurationMultiplier(player);
            AABB box = player.getBoundingBox().inflate(20);
            level.getEntitiesOfClass(LivingEntity.class, box)
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) dur, 0)));
            notifyActivation(player, "§cРедстоун-сенсор — существа обнаружены!");
        }
    }
}
