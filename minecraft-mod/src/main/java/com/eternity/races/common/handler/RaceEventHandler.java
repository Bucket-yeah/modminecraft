package com.eternity.races.common.handler;

import com.eternity.races.common.cap.RaceData;
import com.eternity.races.common.config.RacesConfig;
import com.eternity.races.common.item.ModItems;
import com.eternity.races.common.network.SyncRaceDataPacket;
import com.eternity.races.common.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Главный обработчик событий сервера:
 * - Первый вход: приветствие + книга
 * - Начисление очков за убийства
 * - Пассивные эффекты и штрафы всех 15 рас (каждый тик)
 */
public class RaceEventHandler {

    // Счётчик тиков для оптимизации (не каждый тик делаем всё)
    private static final int TICK_RATE = 20; // раз в секунду

    // ─── Первый вход ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RacesConfig.COMMON.enableRaces.get()) return;

        RaceData data = player.getData(ModAttachments.RACE_DATA.get());

        if (!data.hasChosen()) {
            // Приветственное сообщение
            if (RacesConfig.COMMON.showWelcomeMessage.get()) {
                player.sendSystemMessage(Component.literal("§6⚔ Добро пожаловать в мир Рас! §r"));
                player.sendSystemMessage(Component.literal("§7Выбери свою расу – она определит твой путь."));
                player.sendSystemMessage(Component.literal("§aИспользуй книгу §b«Том рас»§a для открытия меню выбора."));
                player.sendSystemMessage(Component.literal("§cВнимание: выбор расы окончателен!"));
            }
            // Выдаём книгу
            if (RacesConfig.COMMON.grantBookOnJoin.get()) {
                boolean hasBook = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    if (player.getInventory().getItem(i).getItem() == ModItems.RACE_SELECTION_BOOK.get()) {
                        hasBook = true;
                        break;
                    }
                }
                if (!hasBook) {
                    player.addItem(new ItemStack(ModItems.RACE_SELECTION_BOOK.get()));
                }
            }
        }

        // Синхронизируем данные
        PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
    }

    // ─── Смерть игрока ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Данные сохраняются через copyOnDeath в AttachmentType
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(
                player.getData(ModAttachments.RACE_DATA.get())));
    }

    // ─── Убийство мобов → начисление очков ──────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player)) return;
        if (!RacesConfig.COMMON.enableRaces.get()) return;

        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (!data.hasChosen()) return;

        Entity killed = event.getEntity();
        double mult = RacesConfig.COMMON.pointGainMultiplier.get();

        // Базовые очки за моба
        int points = getKillPoints(killed, data.getRaceId(), player);
        if (points > 0) {
            data.addPoints((int) Math.round(points * mult));
            PacketDistributor.sendToPlayer(player, SyncRaceDataPacket.from(data));
        }
    }

    private static int getKillPoints(Entity killed, int raceId, Player player) {
        int base = 1;
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType()).getPath();

        // Боссы
        if (type.equals("wither") || type.equals("ender_dragon")) return 50;

        // Сильные мобы
        if (type.equals("enderman") || type.equals("blaze") || type.equals("wither_skeleton")
                || type.equals("piglin_brute")) return 3;

        // Расовые бонусы
        boolean isNight = !player.level().isDay();
        base += switch (raceId) {
            case 5 -> player.isSprinting() ? 2 : 0;                              // Кинетик
            case 6 -> isNight ? 2 : 0;                                            // Лунный Жнец
            case 9 -> player.getPersistentData().getBoolean("shadow_controlling") ? 2 : 0; // Теневой Дипломат
            case 15 -> player.getBlockY() < 40 ? 2 : 0;                          // Глубинный Гном
            default -> 0;
        };
        return base;
    }

    // ─── Тиковые пассивы и штрафы ────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!RacesConfig.COMMON.enableRaces.get()) return;

        RaceData data = player.getData(ModAttachments.RACE_DATA.get());
        if (!data.hasChosen()) return;

        long tick = player.level().getGameTime();

        switch (data.getRaceId()) {
            case 1  -> tickEchoGolem(player, data, tick);
            case 2  -> tickMnemoEngineer(player, data, tick);
            case 3  -> tickChimeraSymbiont(player, data, tick);
            case 4  -> tickDimensionWeaver(player, data, tick);
            case 5  -> tickKineticPyromancer(player, data, tick);
            case 6  -> tickLunarReaper(player, data, tick);
            case 7  -> tickGravityWeaver(player, data, tick);
            case 8  -> tickEntropyDestroyer(player, data, tick);
            case 9  -> tickShadowDiplomat(player, data, tick);
            case 10 -> tickAlchemist(player, data, tick);
            case 11 -> tickGardener(player, data, tick);
            case 12 -> tickWanderer(player, data, tick);
            case 13 -> tickRedstone(player, data, tick);
            case 14 -> tickMercantile(player, data, tick);
            case 15 -> tickDeepGnome(player, data, tick);
        }
    }

    // ── Раса 1: Эхо-Голем ────────────────────────────────────────────────────
    private static void tickEchoGolem(Player p, RaceData d, long tick) {
        // Голод 1.5× при беге
        if (p.isSprinting()) p.getFoodData().addExhaustion(0.015f);
        // Ослабление на шерсти/слизе
        Block under = p.level().getBlockState(BlockPos.containing(p.position()).below()).getBlock();
        boolean weakened = (under == Blocks.WHITE_WOOL || under == Blocks.SLIME_BLOCK
                || under.getSoundType(p.level().getBlockState(BlockPos.containing(p.position()).below()),
                p.level(), BlockPos.containing(p.position()).below(), p) == SoundType.WOOL);
        p.getPersistentData().putBoolean("1_weakened", weakened);
    }

    // ── Раса 2: Мнемо-Инженер ────────────────────────────────────────────────
    private static void tickMnemoEngineer(Player p, RaceData d, long tick) {
        // Инвентарь перемешивается каждые 2 мин
        if (tick % 2400 == 0 && tick > 0) {
            shuffleInventory(p);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1));
        }
    }

    private static void shuffleInventory(Player p) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty()) { items.add(s.copy()); p.getInventory().setItem(i, ItemStack.EMPTY); }
        }
        java.util.Collections.shuffle(items);
        int idx = 0;
        for (ItemStack s : items) {
            while (idx < 36 && !p.getInventory().getItem(idx).isEmpty()) idx++;
            if (idx < 36) p.getInventory().setItem(idx, s);
        }
    }

    // ── Раса 3: Химера-Симбионт ──────────────────────────────────────────────
    private static void tickChimeraSymbiont(Player p, RaceData d, long tick) {
        // Реген вне боя
        boolean notInCombat = p.getLastHurtByMob() == null || (p.tickCount - p.getLastHurtByMobTimestamp()) > 100;
        if (tick % 100 == 0 && notInCombat && p.getHealth() < p.getMaxHealth()) {
            p.heal(1f);
        }
        // Штраф: голод < 3 → урон
        if (tick % 1200 == 0 && p.getFoodData().getFoodLevel() < 3) {
            p.hurt(p.damageSources().starve(), 1f);
        }
    }

    // ── Раса 4: Ткач Измерений ───────────────────────────────────────────────
    private static void tickDimensionWeaver(Player p, RaceData d, long tick) {
        // Эндер жемчуги не работают — логика в отдельном ивенте (ProjectileImpactEvent)
    }

    // ── Раса 5: Кинетик-Пиромант ─────────────────────────────────────────────
    private static void tickKineticPyromancer(Player p, RaceData d, long tick) {
        // Иммунитет к огню
        if (p.isOnFire()) p.clearFire();
        // Стояние на месте > 3 сек → урон
        long standingTicks = p.getPersistentData().getLong("pyro_stand_ticks");
        if (p.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
            p.getPersistentData().putLong("pyro_stand_ticks", standingTicks + 1);
            if (standingTicks > 60 && tick % 20 == 0) {
                p.hurt(p.damageSources().magic(), 1f);
            }
        } else {
            p.getPersistentData().putLong("pyro_stand_ticks", 0);
        }
        // Вода → урон
        if (p.isInWater() && tick % 20 == 0) p.hurt(p.damageSources().drown(), 1f);
    }

    // ── Раса 6: Лунный Жнец ──────────────────────────────────────────────────
    private static void tickLunarReaper(Player p, RaceData d, long tick) {
        boolean night = !p.level().isDay();
        if (night) {
            // Ночь: 20 хп, +30% скорость, пассивный реген
            p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                    .setBaseValue(20.0);
            if (tick % 100 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1f);
        } else {
            // День: 12 хп, -20% скорость
            double maxHp = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
            if (maxHp > 12.0) {
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(12.0);
                if (p.getHealth() > 12f) p.setHealth(12f);
            }
        }
        // Штраф: не убивал этой ночью → -3 макс хп (в конце ночи)
        if (!night && p.level().getDayTime() == 0) {
            boolean killedThisNight = p.getPersistentData().getBoolean("lunar_killed_tonight");
            if (!killedThisNight) {
                double current = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                        .setBaseValue(Math.max(6.0, current - 3.0));
            }
            p.getPersistentData().putBoolean("lunar_killed_tonight", false);
        }
    }

    // ── Раса 7: Грав-Ткач ────────────────────────────────────────────────────
    private static void tickGravityWeaver(Player p, RaceData d, long tick) {
        // Двойной урон от утопания обрабатывается в отдельном ивенте
    }

    // ── Раса 8: Энтропийный Разрушитель ──────────────────────────────────────
    private static void tickEntropyDestroyer(Player p, RaceData d, long tick) {
        // Нельзя ставить блоки — обрабатывается в BlockPlaceEvent
        // Случайный блок ломается каждые 5 мин
        if (tick % 6000 == 100) {
            BlockPos pos = p.blockPosition().offset(
                    (int)(Math.random() * 5 - 2), (int)(Math.random() * 3 - 1), (int)(Math.random() * 5 - 2));
            if (!p.level().getBlockState(pos).isAir() && !p.level().getBlockState(pos).is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
                p.level().destroyBlock(pos, true, p);
            }
        }
    }

    // ── Раса 9: Теневой Дипломат ─────────────────────────────────────────────
    private static void tickShadowDiplomat(Player p, RaceData d, long tick) {
        // Невидимость для мобов без брони и без атаки
        boolean hasArmor = !p.getInventory().armor.stream().allMatch(ItemStack::isEmpty);
        boolean inCombat = p.getLastHurtByMob() != null && (p.tickCount - p.getLastHurtByMobTimestamp()) <= 100;
        if (!hasArmor && !inCombat) {
            // Снимаем агро ближайших мобов
            if (tick % 40 == 0) {
                p.level().getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(8))
                        .forEach(mob -> { if (mob.getTarget() == p) mob.setTarget(null); });
            }
        }
    }

    // ── Раса 10: Алхимик-Трансмутатор ────────────────────────────────────────
    private static void tickAlchemist(Player p, RaceData d, long tick) {
        // Инвентарь меняется каждые 5 мин (менее радикально, чем у Мнемо-Инженера)
        if (tick % 6000 == 300) swapTwoRandomItems(p);
    }

    private static void swapTwoRandomItems(Player p) {
        java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) if (!p.getInventory().getItem(i).isEmpty()) nonEmpty.add(i);
        if (nonEmpty.size() < 2) return;
        java.util.Collections.shuffle(nonEmpty);
        int a = nonEmpty.get(0), b = nonEmpty.get(1);
        ItemStack tmp = p.getInventory().getItem(a).copy();
        p.getInventory().setItem(a, p.getInventory().getItem(b).copy());
        p.getInventory().setItem(b, tmp);
    }

    // ── Раса 11: Садовник-Симбиот ─────────────────────────────────────────────
    private static void tickGardener(Player p, RaceData d, long tick) {
        // Реген на солнце
        if (p.level().isDay() && p.level().canSeeSky(p.blockPosition()) && tick % 60 == 0) {
            if (p.getHealth() < p.getMaxHealth()) p.heal(0.5f);
            p.getFoodData().eat(0, 0.5f);
        }
        // Вода → урон
        if (p.isInWater() && tick % 20 == 0) p.hurt(p.damageSources().drown(), 1f);
    }

    // ── Раса 12: Странник-Картограф ──────────────────────────────────────────
    private static void tickWanderer(Player p, RaceData d, long tick) {
        // Бег без расхода голода
        if (p.isSprinting()) p.getFoodData().addExhaustion(-0.015f);
    }

    // ── Раса 13: Редстоун-Инженер ────────────────────────────────────────────
    private static void tickRedstone(Player p, RaceData d, long tick) {
        // Ускорение I при держании редстоуна
        boolean holdingRedstone = p.getMainHandItem().is(net.minecraft.world.item.Items.REDSTONE)
                || p.getOffhandItem().is(net.minecraft.world.item.Items.REDSTONE);
        if (holdingRedstone) {
            if (!p.hasEffect(MobEffects.DIG_SPEED)) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false));
            }
            // Удар молнии если держит > 30 сек
            long holdTicks = p.getPersistentData().getLong("redstone_hold_ticks") + 1;
            p.getPersistentData().putLong("redstone_hold_ticks", holdTicks);
            if (holdTicks > 600 && Math.random() < 0.001) {
                p.level().levelEvent(null, 3002, p.blockPosition(), -1);
                p.hurt(p.damageSources().lightningBolt(), 4f);
                p.getPersistentData().putLong("redstone_hold_ticks", 0);
            }
        } else {
            p.getPersistentData().putLong("redstone_hold_ticks", 0);
        }
    }

    // ── Раса 14: Меркантильный Маг ───────────────────────────────────────────
    private static void tickMercantile(Player p, RaceData d, long tick) {
        // Логика скидки реализуется через ивент торговли жителей
    }

    // ── Раса 15: Глубинный Гном ──────────────────────────────────────────────
    private static void tickDeepGnome(Player p, RaceData d, long tick) {
        // Ночное зрение всегда
        if (!p.hasEffect(MobEffects.NIGHT_VISION) || p.getEffect(MobEffects.NIGHT_VISION).getDuration() < 40) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
        }
        // Выше Y=60 → урон + замедление
        if (p.getBlockY() > 60) {
            if (!p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            }
            if (tick % 200 == 0) p.hurt(p.damageSources().magic(), 1f);
        }
    }
}
