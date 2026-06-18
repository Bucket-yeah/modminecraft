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

    // 0. Алхимия предметов — 4 предмета → случайный ресурс
    public static class ItemAlchemy extends AbstractAbility {
        public ItemAlchemy() {
            super(id("item_alchemy"), "ability.racecraft.item_alchemy.name",
                    "ability.racecraft.item_alchemy.desc", 300, 0, 14, 0);
        }
        @Override
        public void execute(Player player, Level level) {
            // Проверяем наличие изумруда (стоимость)
            if (!hasItem(player, Items.EMERALD, 1)) {
                notifyActivation(player, "§eНужен изумруд для алхимии!");
                return;
            }
            removeItem(player, Items.EMERALD, 1);
            int consumed = 0;
            for (int i = 0; i < 36 && consumed < 4; i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    player.getInventory().getItem(i).shrink(1);
                    consumed++;
                }
            }
            if (consumed < 4) { notifyActivation(player, "§eНедостаточно предметов!"); return; }
            // Случайный ресурс
            ItemStack[] rewards = {
                new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 3)
            };
            player.addItem(rewards[level.random.nextInt(rewards.length)]);
            notifyActivation(player, "§6Алхимия предметов!");
        }
    }

    // 1. Улучшение жителя — повышение уровня жителя
    public static class VillagerUpgrade extends AbstractAbility {
        public VillagerUpgrade() {
            super(id("villager_upgrade"), "ability.racecraft.villager_upgrade.name",
                    "ability.racecraft.villager_upgrade.desc", 1200, 0, 14, 1);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.GOLDEN_APPLE, 1)) {
                notifyActivation(player, "§eНужно золотое яблоко!");
                return;
            }
            AABB box = player.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(Villager.class, box).stream().findFirst()
                    .ifPresent(v -> {
                        removeItem(player, Items.GOLDEN_APPLE, 1);
                        v.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                        notifyActivation(player, "§6Житель улучшен!");
                    });
        }
    }

    // 2. Валютный обмен — изумруды → ресурсы
    public static class CurrencyExchange extends AbstractAbility {
        public CurrencyExchange() {
            super(id("currency_exchange"), "ability.racecraft.currency_exchange.name",
                    "ability.racecraft.currency_exchange.desc", 200, 0, 14, 2);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.EMERALD, 5)) {
                notifyActivation(player, "§eНужно 5 изумрудов!");
                return;
            }
            removeItem(player, Items.EMERALD, 5);
            player.addItem(new ItemStack(Items.IRON_INGOT, 10));
            notifyActivation(player, "§65 изумрудов → 10 железных слитков!");
        }
    }

    // 3. Торговый импульс — мгновенная торговля 10 сек (Ветка A)
    public static class TradeImpulse extends AbstractAbility {
        public TradeImpulse() {
            super(id("trade_impulse"), "ability.racecraft.trade_impulse.name",
                    "ability.racecraft.trade_impulse.desc", 400, 20, 14, 3);
        }
        @Override
        public void execute(Player player, Level level) {
            float dur = 200 * getDurationMultiplier(player);
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, (int) dur, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) dur, 1));
            player.getPersistentData().putLong("trade_impulse_expire", level.getGameTime() + (long) dur);
            notifyActivation(player, "§6Торговый импульс — скидки 30 сек!");
        }
    }

    // 4. Привлечение ресурса — указывает на ценный ресурс (Ветка A)
    public static class ResourceAttraction extends AbstractAbility {
        public ResourceAttraction() {
            super(id("resource_attraction"), "ability.racecraft.resource_attraction.name",
                    "ability.racecraft.resource_attraction.desc", 300, 20, 14, 4);
        }
        @Override
        public void execute(Player player, Level level) {
            AABB box = player.getBoundingBox().inflate(30);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)
                    .forEach(item -> item.setGlowingTag(true));
            notifyActivation(player, "§6Привлечение ресурса — предметы подсвечены!");
        }
    }

    // 5. Переработка мусора — хлам → полезный блок (Ветка B)
    public static class JunkRecycling extends AbstractAbility {
        public JunkRecycling() {
            super(id("junk_recycling"), "ability.racecraft.junk_recycling.name",
                    "ability.racecraft.junk_recycling.desc", 200, 20, 14, 5);
        }
        @Override
        public void execute(Player player, Level level) {
            // Превращаем гниющую плоть в кожу
            for (int i = 0; i < 36; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.ROTTEN_FLESH)) {
                    int count = stack.getCount();
                    player.getInventory().setItem(i, new ItemStack(Items.LEATHER, count));
                    notifyActivation(player, "§6Переработка: гнилая плоть → кожа ×" + count);
                    return;
                }
            }
            notifyActivation(player, "§eНет подходящего хлама.");
        }
    }

    // 6. Защитная сделка — нанять моба на 1 мин (Ветка B)
    public static class ProtectiveDeal extends AbstractAbility {
        public ProtectiveDeal() {
            super(id("protective_deal"), "ability.racecraft.protective_deal.name",
                    "ability.racecraft.protective_deal.desc", 1200, 20, 14, 6);
        }
        @Override
        public void execute(Player player, Level level) {
            if (!hasItem(player, Items.EMERALD, 3)) {
                notifyActivation(player, "§eНужно 3 изумруда!");
                return;
            }
            removeItem(player, Items.EMERALD, 3);
            AABB box = player.getBoundingBox().inflate(10);
            level.getEntitiesOfClass(Villager.class, box).stream().findFirst()
                    .ifPresent(v -> {
                        v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 2));
                        v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                        notifyActivation(player, "§6Житель нанят в охранники!");
                    });
        }
    }

    // 7. Бартер — XP → изумруды/алмазы (Tier 3)
    public static class Barter extends AbstractAbility {
        public Barter() {
            super(id("barter"), "ability.racecraft.barter.name",
                    "ability.racecraft.barter.desc", 400, 30, 14, 7);
        }
        @Override
        public void execute(Player player, Level level) {
            int xpLevels = player.experienceLevel;
            if (xpLevels < 5) {
                notifyActivation(player, "§eНужно минимум 5 уровней XP!");
                return;
            }
            player.giveExperienceLevels(-5);
            if (Math.random() < 0.3) {
                player.addItem(new ItemStack(Items.DIAMOND, 1));
                notifyActivation(player, "§6Бартер: 5 уровней XP → алмаз!");
            } else {
                player.addItem(new ItemStack(Items.EMERALD, 3));
                notifyActivation(player, "§6Бартер: 5 уровней XP → 3 изумруда!");
            }
        }
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item, int count) {
        return player.getInventory().countItem(item) >= count;
    }

    private static void removeItem(Player player, net.minecraft.world.item.Item item, int count) {
        int toRemove = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(toRemove, stack.getCount());
                stack.shrink(take);
                toRemove -= take;
            }
        }
    }
}
