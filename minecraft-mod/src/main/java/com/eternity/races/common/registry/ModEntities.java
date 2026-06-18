package com.eternity.races.common.registry;

import com.eternity.races.RacesMod;
import com.eternity.races.common.mob.SummonedSlime;
import com.eternity.races.common.mob.SummonedSpider;
import com.eternity.races.common.mob.SummonedWolf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Регистрация EntityType для всех призываемых существ мода.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, RacesMod.MOD_ID);

    // Призванный волк (Лунный Жнец)
    public static final Supplier<EntityType<SummonedWolf>> SUMMONED_WOLF =
            ENTITY_TYPES.register("summoned_wolf", () ->
                    EntityType.Builder.<SummonedWolf>of(SummonedWolf::new, MobCategory.CREATURE)
                            .sized(0.6f, 0.85f)
                            .clientTrackingRange(8)
                            .build("summoned_wolf")
            );

    // Призванный паук (Глубинный Гном)
    public static final Supplier<EntityType<SummonedSpider>> SUMMONED_SPIDER =
            ENTITY_TYPES.register("summoned_spider", () ->
                    EntityType.Builder.<SummonedSpider>of(SummonedSpider::new, MobCategory.CREATURE)
                            .sized(1.4f, 0.9f)
                            .clientTrackingRange(8)
                            .build("summoned_spider")
            );

    // Призванный слизень (Химера-Симбионт)
    public static final Supplier<EntityType<SummonedSlime>> SUMMONED_SLIME =
            ENTITY_TYPES.register("summoned_slime", () ->
                    EntityType.Builder.<SummonedSlime>of(SummonedSlime::new, MobCategory.CREATURE)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(8)
                            .build("summoned_slime")
            );

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
