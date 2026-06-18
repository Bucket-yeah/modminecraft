package com.eternity.races.common.ability.abilities;

import com.eternity.races.RacesMod;
import com.eternity.races.common.ability.AbstractAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/** Раса 14 — Меркантильный Маг: все 8 способностей */
public class MercantileMageAbilities {

    public static void registerAll(Consumer<AbstractAbility> reg) {
        reg.accept(new ItemAlchemy());
        reg.accept(new VillagerUpgrade());
        reg.accept(new CurrencyExchange());
        reg.accept(new TradeImpulse());
        reg.accept(new ResourceAttraction());
        reg.accept(new JunkRecycling());
        reg.accept(new ProtectiveDeal());
        reg.accept(new Barter());
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(RacesMod.MOD_ID, name);
    }

    /** 14.1 Алхимия предметов — изумруд → случайный редкий предмет (100t, 1 изумруд) */
    public static class ItemAlchemy extends AbstractAbility {
        public ItemAlchemy() {
            super(id("item_alchemy"), "ability.racecraft.item_alchemy.name",
                    "ability.racecraft.item_alchemy.desc", 100, 0, 14, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.EMERALD, 1)) {
                notifyActivation(player, "§6Нужен 1 изумруд!"); return;
            }
            removeItem(player, Items.EMERALD, 1);
            int lvl = getAccessoryLevel(player);
            int r = level.random.nextInt(10 + lvl);
            ItemStack result;
            if (r < 2) result = new ItemStack(Items.DIAMOND, 1);
            else if (r < 4) result = new ItemStack(Items.GOLD_INGOT, 3 + lvl);
            else if (r < 6) result = new ItemStack(Items.IRON_INGOT, 5 + lvl);
            else if (r < 8) result = new ItemStack(Items.LAPIS_LAZULI, 10);
            else if (r == 8) result = new ItemStack(Items.ENCHANTED_BOOK);
            else result = new ItemStack(Items.EMERALD, 3 + lvl / 2);
            player.addItem(result);
            notifyActivation(player, "§6Алхимия! Получен: " + result.getCount() + "× " + result.getDescriptionId());
        }
    }

    /** 14.2 Улучшение торговца — прокачка жителя (24000t, 1 золотое яблоко) */
    public static class VillagerUpgrade extends AbstractAbility {
        public VillagerUpgrade() {
            super(id("villager_upgrade"), "ability.racecraft.villager_upgrade.name",
                    "ability.racecraft.villager_upgrade.desc", 24000, 0, 14, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.GOLDEN_APPLE, 1)) {
                notifyActivation(player, "§6Нужно золотое яблоко!"); return;
            }
            AABB box = player.getBoundingBox().inflate(5);
            var villagers = level.getEntitiesOfClass(Villager.class, box);
            if (villagers.isEmpty()) { notifyActivation(player, "§cНет жителей рядом!"); return; }
            removeItem(player, Items.GOLDEN_APPLE, 1);
            Villager v = villagers.get(0);
            v.setVillagerData(v.getVillagerData().setLevel(Math.min(5, v.getVillagerData().getLevel() + 1)));
            notifyActivation(player, "§6Улучшение торговца! Уровень: " + v.getVillagerData().getLevel());
        }
    }

    /** 14.3 Обмен валюты — изумруды → золото (1200t, 5 изумрудов) */
    public static class CurrencyExchange extends AbstractAbility {
        public CurrencyExchange() {
            super(id("currency_exchange"), "ability.racecraft.currency_exchange.name",
                    "ability.racecraft.currency_exchange.desc", 1200, 0, 14, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            int emeraldCount = player.getInventory().countItem(Items.EMERALD);
            if (emeraldCount < 5) { notifyActivation(player, "§6Нужно 5+ изумрудов!"); return; }
            int exchange = (emeraldCount / 5) * 5;
            removeItem(player, Items.EMERALD, exchange);
            int gold = (exchange / 5) * (8 + getAccessoryLevel(player));
            player.addItem(new ItemStack(Items.GOLD_INGOT, gold));
            notifyActivation(player, "§6Обмен! " + exchange + " изумрудов → " + gold + " золота.");
        }
    }

    /** 14.4 Торговый импульс — скидка 30% у жителей 2 мин (600t, 1 голод) */
    public static class TradeImpulse extends AbstractAbility {
        public TradeImpulse() {
            super(id("trade_impulse"), "ability.racecraft.trade_impulse.name",
                    "ability.racecraft.trade_impulse.desc", 600, 20, 14, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            int dur = (int)(2400 * getDurationMultiplier(player));
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, dur, 2));
            player.getPersistentData().putLong("trade_discount_expire", level.getGameTime() + dur);
            notifyActivation(player, "§6Торговый импульс! Скидка у жителей на 2 мин.");
        }
    }

    /** 14.5 Притяжение ресурсов — близлежащие дропы летят к игроку (400t, 1 голод) */
    public static class ResourceAttraction extends AbstractAbility {
        public ResourceAttraction() {
            super(id("resource_attraction"), "ability.racecraft.resource_attraction.name",
                    "ability.racecraft.resource_attraction.desc", 400, 20, 14, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeHunger(player, 1)) return;
            float radius = 15f * (1f + getAccessoryLevel(player) * 0.1f);
            AABB box = player.getBoundingBox().inflate(radius);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)
                    .forEach(item -> {
                        item.playerTouch(player);
                    });
            notifyActivation(player, "§6Притяжение ресурсов! Предметы в " + (int) radius + " блоках подобраны.");
        }
    }

    /** 14.6 Переработка хлама — дроп → изумруды (40t, любой предмет) */
    public static class JunkRecycling extends AbstractAbility {
        public JunkRecycling() {
            super(id("junk_recycling"), "ability.racecraft.junk_recycling.name",
                    "ability.racecraft.junk_recycling.desc", 40, 20, 14, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            int recycled = 0;
            for (int i = 0; i < 36 && recycled < 8; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && !stack.is(Items.EMERALD) && !stack.is(Items.DIAMOND)) {
                    int count = stack.getCount();
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    recycled += count;
                }
            }
            if (recycled == 0) { notifyActivation(player, "§cНет предметов для переработки!"); return; }
            int emeralds = Math.max(1, recycled / 4 + getAccessoryLevel(player));
            player.addItem(new ItemStack(Items.EMERALD, emeralds));
            notifyActivation(player, "§6Переработка! " + recycled + " предм. → " + emeralds + " изумрудов.");
        }
    }

    /** 14.7 Защитная сделка — 3 изумруда → щит 5 мин (2400t, 3 изумруда) */
    public static class ProtectiveDeal extends AbstractAbility {
        public ProtectiveDeal() {
            super(id("protective_deal"), "ability.racecraft.protective_deal.name",
                    "ability.racecraft.protective_deal.desc", 2400, 20, 14, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.EMERALD, 3)) {
                notifyActivation(player, "§6Нужно 3 изумруда!"); return;
            }
            removeItem(player, Items.EMERALD, 3);
            int dur = (int)(6000 * getDurationMultiplier(player));
            int absLevel = 4 + getAccessoryLevel(player) / 2;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, absLevel));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1));
            player.getPersistentData().putLong("protective_deal_expire", level.getGameTime() + dur);
            notifyActivation(player, "§6Защитная сделка! Поглощение + защита на 5 мин. (-3 изумруда)");
        }
    }

    /** 14.8 Бартер — 5 XP уровней → случайные предметы (200t, 5 XP уровней) */
    public static class Barter extends AbstractAbility {
        public Barter() {
            super(id("barter"), "ability.racecraft.barter.name",
                    "ability.racecraft.barter.desc", 200, 30, 14, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!consumeXp(player, 5)) return;
            int lvl = getAccessoryLevel(player);
            int count = 3 + lvl / 3;
            for (int i = 0; i < count; i++) {
                ItemStack item;
                int r = level.random.nextInt(8 + lvl);
                if (r < 2) item = new ItemStack(Items.GOLD_INGOT, 3 + lvl);
                else if (r < 4) item = new ItemStack(Items.IRON_INGOT, 5 + lvl);
                else if (r < 6) item = new ItemStack(Items.EMERALD, 2 + lvl / 2);
                else if (r == 6) item = new ItemStack(Items.DIAMOND, 1);
                else item = new ItemStack(Items.LAPIS_LAZULI, 8);
                player.addItem(item);
            }
            notifyActivation(player, "§6Бартер! " + count + " предметов за 5 XP уровней.");
        }
    }

    private static int getAccessoryLevel(Player player) {
        return player.getData(com.eternity.races.common.registry.ModAttachments.RACE_DATA.get()).getAccessoryLevel();
    }
}
