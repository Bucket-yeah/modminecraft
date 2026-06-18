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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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

    /** 13.1 Усиление сигнала — +2 редстоун-силы в радиусе 10 (200t, 1 голод) */
    public static class SignalBoost extends AbstractAbility {
        public SignalBoost() {
            super(id("signal_boost"), "ability.racecraft.signal_boost.name",
                    "ability.racecraft.signal_boost.desc", 200, 0, 13, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int radius = 10 + getAccessoryLevel(player);
            int dur = (int)(200 * getDurationMultiplier(player));
            // Активируем все рычаги и кнопки рядом — даём ускорение добычи
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0));
            // Симулируем "сигнал" — подаём питание блокам
            for (BlockPos pos : BlockPos.betweenClosed(
                    player.blockPosition().offset(-radius, -2, -radius),
                    player.blockPosition().offset(radius, 2, radius))) {
                if (level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
                    level.setBlockAndUpdate(pos, level.getBlockState(pos));
                }
            }
            notifyActivation(player, "§c Усиление сигнала! Ускорение добычи II + скорость.");
        }
    }

    /** 13.2 Временный провод — 5 блоков редстоуна на 30 сек (400t, 1 голод) */
    public static class TempWire extends AbstractAbility {
        public TempWire() {
            super(id("temp_wire"), "ability.racecraft.temp_wire.name",
                    "ability.racecraft.temp_wire.desc", 400, 0, 13, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            if (level.isClientSide) return;
            int len = 5 + getAccessoryLevel(player) / 2;
            var dir = player.getDirection();
            BlockPos start = player.blockPosition();
            for (int i = 1; i <= len; i++) {
                BlockPos pos = start.relative(dir, i);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                    level.setBlockAndUpdate(pos, Blocks.REDSTONE_WIRE.defaultBlockState());
                    long expire = level.getGameTime() + 600;
                    // Запоминаем для очистки через tick
                    player.getPersistentData().putLong("wire_" + i + "_x", pos.getX());
                    player.getPersistentData().putLong("wire_" + i + "_y", pos.getY());
                    player.getPersistentData().putLong("wire_" + i + "_z", pos.getZ());
                    player.getPersistentData().putLong("wire_" + i + "_expire", expire);
                }
            }
            player.getPersistentData().putInt("wire_count", len);
            notifyActivation(player, "§cВременный провод! " + len + " блоков редстоуна на 30 сек.");
        }
    }

    /** 13.3 Глушитель — отключение редстоун-устройств в 5 блоках 10 сек (300t, 1 голод) */
    public static class Jammer extends AbstractAbility {
        public Jammer() {
            super(id("jammer"), "ability.racecraft.jammer.name",
                    "ability.racecraft.jammer.desc", 300, 0, 13, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int radius = 5 + getAccessoryLevel(player) / 2;
            // Глушим мобов (имитация электромагнитного поля)
            int dur = (int)(200 * getDurationMultiplier(player));
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
                    });
            player.getPersistentData().putLong("jammer_expire", level.getGameTime() + dur);
            notifyActivation(player, "§cГлушитель! Поле помех " + radius + " блоков, 10 сек.");
        }
    }

    /** 13.4 Сканирование схемы — анализ блоков в 5 блоках (100t, 1 голод) */
    public static class CircuitScan extends AbstractAbility {
        public CircuitScan() {
            super(id("circuit_scan"), "ability.racecraft.circuit_scan.name",
                    "ability.racecraft.circuit_scan.desc", 100, 20, 13, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            // Подсвечиваем редстоун-компоненты и мобов
            int dur = (int)(100 * getDurationMultiplier(player));
            level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(15))
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0)));
            // Сообщение об окружении
            int y = player.getBlockY();
            boolean hasOre = false;
            for (BlockPos pos : BlockPos.betweenClosed(
                    player.blockPosition().offset(-5, -3, -5),
                    player.blockPosition().offset(5, 3, 5))) {
                var block = level.getBlockState(pos).getBlock();
                if (block == Blocks.IRON_ORE || block == Blocks.GOLD_ORE || block == Blocks.DIAMOND_ORE
                        || block == Blocks.EMERALD_ORE || block == Blocks.REDSTONE_ORE) {
                    hasOre = true;
                    break;
                }
            }
            notifyActivation(player, "§cСканирование: Y=" + y + (hasOre ? " §aРуды рядом!" : ""));
        }
    }

    /** 13.5 Быстрый ремонт — предметы в руках восстанавливают прочность (60t, 1 голод) */
    public static class QuickRepair extends AbstractAbility {
        public QuickRepair() {
            super(id("quick_repair"), "ability.racecraft.quick_repair.name",
                    "ability.racecraft.quick_repair.desc", 60, 20, 13, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int repairAmount = 50 + getAccessoryLevel(player) * 10;
            var mainHand = player.getMainHandItem();
            var offHand = player.getOffhandItem();
            if (!mainHand.isEmpty() && mainHand.isDamaged()) {
                mainHand.setDamageValue(Math.max(0, mainHand.getDamageValue() - repairAmount));
                notifyActivation(player, "§cБыстрый ремонт! Главная рука: +" + repairAmount + " прочности.");
            } else if (!offHand.isEmpty() && offHand.isDamaged()) {
                offHand.setDamageValue(Math.max(0, offHand.getDamageValue() - repairAmount));
                notifyActivation(player, "§cБыстрый ремонт! Доп. рука: +" + repairAmount + " прочности.");
            } else {
                notifyActivation(player, "§cНет повреждённых предметов в руках!");
            }
        }
    }

    /** 13.6 Энергетический выброс — разряд в 15 блоках, 8 урон (600t, 3 XP уровня) */
    public static class EnergyBurst extends AbstractAbility {
        public EnergyBurst() {
            super(id("energy_burst"), "ability.racecraft.energy_burst.name",
                    "ability.racecraft.energy_burst.desc", 600, 20, 13, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 3)) return;
            float radius = 15f * (1f + getAccessoryLevel(player) * 0.05f);
            float dmg = 8f * getDamageMultiplier(player);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)
                    .forEach(e -> {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    });
            // Удар молнии
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                var bolt = new net.minecraft.world.entity.LightningBolt(
                        net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, sl);
                bolt.setPos(player.getX(), player.getY(), player.getZ());
                bolt.setVisualOnly(false);
                sl.addFreshEntity(bolt);
            }
            notifyActivation(player, "§cЭнергетический выброс! " + (int) dmg + " урона. -3 XP");
        }
    }

    /** 13.7 Копирование схемы — 5 книг → записать схему (3600t, 3 голода + 5 книг) */
    public static class CircuitCopy extends AbstractAbility {
        public CircuitCopy() {
            super(id("circuit_copy"), "ability.racecraft.circuit_copy.name",
                    "ability.racecraft.circuit_copy.desc", 3600, 20, 13, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 3)) return;
            if (!hasItem(player, Items.BOOK, 5)) {
                notifyActivation(player, "§cНужно 5 книг!");
                return;
            }
            removeItem(player, Items.BOOK, 5);
            // Выдаём зачарованные книги или рецепты
            int lvl = getAccessoryLevel(player);
            int bookCount = 2 + lvl / 3;
            for (int i = 0; i < bookCount; i++) {
                player.addItem(new ItemStack(Items.ENCHANTED_BOOK, 1));
            }
            // Бонус: ускорение добычи
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 2));
            notifyActivation(player, "§cКопирование схемы! +" + bookCount + " зачарованных книг.");
        }
    }

    /** 13.8 Датчик редстоуна — обнаружение редстоун-сигналов в 20 блоках (400t, 1 голод) */
    public static class RedstoneSensor extends AbstractAbility {
        public RedstoneSensor() {
            super(id("redstone_sensor"), "ability.racecraft.redstone_sensor.name",
                    "ability.racecraft.redstone_sensor.desc", 400, 30, 13, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(400 * getDurationMultiplier(player));
            // Подсветка всех существ + ночное зрение
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur, 0, false, false));
            level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20))
                    .forEach(e -> e.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0)));
            // Активируем ближайший датчик
            for (BlockPos pos : BlockPos.betweenClosed(
                    player.blockPosition().offset(-10, -5, -10),
                    player.blockPosition().offset(10, 5, 10))) {
                if (level.getBlockState(pos).is(Blocks.SCULK_SENSOR)) {
                    level.setBlockAndUpdate(pos, level.getBlockState(pos));
                }
            }
            notifyActivation(player, "§cДатчик редстоуна! Подсветка существ в 20 блоках.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
