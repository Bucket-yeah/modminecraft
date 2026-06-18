package com.eternity.races.common.item;

import com.eternity.races.RacesMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Регистрация всех предметов мода EternityRaces.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RacesMod.MOD_ID);

    // ─── Книга выбора расы ───────────────────────────────────────────────────

    public static final DeferredItem<RaceBookItem> RACE_SELECTION_BOOK =
            ITEMS.register("race_selection_book",
                    () -> new RaceBookItem(new Item.Properties().stacksTo(1)));

    // ─── Кольца рас (1-15) ──────────────────────────────────────────────────

    public static final DeferredItem<RaceRingItem> RING_ECHO_GOLEM =
            ITEMS.register("ring_echo_golem",
                    () -> new RaceRingItem(1, "Эхо-Голем",
                            "голод 1.5× при беге, вибрация",
                            "слабость на шерсти, урон от нотных блоков"));

    public static final DeferredItem<RaceRingItem> RING_MNEMO_ENGINEER =
            ITEMS.register("ring_mnemo_engineer",
                    () -> new RaceRingItem(2, "Мнемо-Инженер",
                            "временная перемотка, сохранение точек",
                            "инвентарь перемешивается, еда даёт меньше"));

    public static final DeferredItem<RaceRingItem> RING_CHIMERA_SYMBIONT =
            ITEMS.register("ring_chimera_symbiont",
                    () -> new RaceRingItem(3, "Химера-Симбионт",
                            "реген 1♥/5сек без боя, одержимость",
                            "без брони/оружия, голод → урон"));

    public static final DeferredItem<RaceRingItem> RING_DIMENSION_WEAVER =
            ITEMS.register("ring_dimension_weaver",
                    () -> new RaceRingItem(4, "Ткач Измерений",
                            "пространственные порталы, якоря",
                            "10% ошибка телепорта, Незер 50%"));

    public static final DeferredItem<RaceRingItem> RING_KINETIC_PYROMANCER =
            ITEMS.register("ring_kinetic_pyromancer",
                    () -> new RaceRingItem(5, "Кинетик-Пиромант",
                            "иммунитет к огню, огненный след",
                            "стоять >3сек = урон, вода = урон"));

    public static final DeferredItem<RaceRingItem> RING_LUNAR_REAPER =
            ITEMS.register("ring_lunar_reaper",
                    () -> new RaceRingItem(6, "Лунный Жнец",
                            "ночь: 20♥, +30% скорость; призыв волков",
                            "день: -20% скорость; нужно убивать ночью"));

    public static final DeferredItem<RaceRingItem> RING_GRAVITY_WEAVER =
            ITEMS.register("ring_gravity_weaver",
                    () -> new RaceRingItem(7, "Грав-Ткач",
                            "гравитационные манипуляции, левитация",
                            "двойной урон от падения, утопание 2×"));

    public static final DeferredItem<RaceRingItem> RING_ENTROPY_DESTROYER =
            ITEMS.register("ring_entropy_destroyer",
                    () -> new RaceRingItem(8, "Энтропийный Разрушитель",
                            "мгновенное разрушение блоков, взрывы",
                            "нельзя ставить блоки, 18 слотов"));

    public static final DeferredItem<RaceRingItem> RING_SHADOW_DIPLOMAT =
            ITEMS.register("ring_shadow_diplomat",
                    () -> new RaceRingItem(9, "Теневой Дипломат",
                            "невидимость для мобов без брони",
                            "атака = все мобы агрессивны 1 мин"));

    public static final DeferredItem<RaceRingItem> RING_ALCHEMIST_TRANSMUTOR =
            ITEMS.register("ring_alchemist_transmutor",
                    () -> new RaceRingItem(10, "Алхимик-Трансмутатор",
                            "трансмутация блоков, алхимические щиты",
                            "нельзя крафтить, 10% взрыв"));

    public static final DeferredItem<RaceRingItem> RING_GARDENER_SYMBIONT =
            ITEMS.register("ring_gardener_symbiont",
                    () -> new RaceRingItem(11, "Садовник-Симбиот",
                            "реген на солнце, рост урожая",
                            "вода=урон, нельзя мясо, только дерево"));

    public static final DeferredItem<RaceRingItem> RING_WANDERER_CARTOGRAPHER =
            ITEMS.register("ring_wanderer_cartographer",
                    () -> new RaceRingItem(12, "Странник-Картограф",
                            "координаты, автокарта, бег без голода",
                            "спавн = мировой, карта 500×500"));

    public static final DeferredItem<RaceRingItem> RING_REDSTONE_ENGINEER =
            ITEMS.register("ring_redstone_engineer",
                    () -> new RaceRingItem(13, "Редстоун-Инженер",
                            "видит сигнал, Ускорение I на редстоуне",
                            "5% шанс возгорания машин, молния"));

    public static final DeferredItem<RaceRingItem> RING_MERCANTILE_MAGE =
            ITEMS.register("ring_mercantile_mage",
                    () -> new RaceRingItem(14, "Меркантильный Маг",
                            "скидка 30% у жителей, торговля",
                            "нельзя добыть алмазы/изумруды"));

    public static final DeferredItem<RaceRingItem> RING_DEEP_GNOME =
            ITEMS.register("ring_deep_gnome",
                    () -> new RaceRingItem(15, "Глубинный Гном",
                            "ночное зрение, руды через стены",
                            "выше Y=60: урон + медлительность"));

    // ─── Вспомогательный метод ──────────────────────────────────────────────

    /**
     * Возвращает ItemStack кольца для заданной расы.
     */
    public static ItemStack getRingForRace(int raceId) {
        return switch (raceId) {
            case 1  -> new ItemStack(RING_ECHO_GOLEM.get());
            case 2  -> new ItemStack(RING_MNEMO_ENGINEER.get());
            case 3  -> new ItemStack(RING_CHIMERA_SYMBIONT.get());
            case 4  -> new ItemStack(RING_DIMENSION_WEAVER.get());
            case 5  -> new ItemStack(RING_KINETIC_PYROMANCER.get());
            case 6  -> new ItemStack(RING_LUNAR_REAPER.get());
            case 7  -> new ItemStack(RING_GRAVITY_WEAVER.get());
            case 8  -> new ItemStack(RING_ENTROPY_DESTROYER.get());
            case 9  -> new ItemStack(RING_SHADOW_DIPLOMAT.get());
            case 10 -> new ItemStack(RING_ALCHEMIST_TRANSMUTOR.get());
            case 11 -> new ItemStack(RING_GARDENER_SYMBIONT.get());
            case 12 -> new ItemStack(RING_WANDERER_CARTOGRAPHER.get());
            case 13 -> new ItemStack(RING_REDSTONE_ENGINEER.get());
            case 14 -> new ItemStack(RING_MERCANTILE_MAGE.get());
            case 15 -> new ItemStack(RING_DEEP_GNOME.get());
            default -> ItemStack.EMPTY;
        };
    }
}
