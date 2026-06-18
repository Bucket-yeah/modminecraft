package com.eternity.races;

import com.eternity.races.client.ClientSetup;
import com.eternity.races.common.config.RacesConfig;
import com.eternity.races.common.handler.RaceEventHandler;
import com.eternity.races.common.item.ModItems;
import com.eternity.races.common.mob.SummonedSlime;
import com.eternity.races.common.mob.SummonedSpider;
import com.eternity.races.common.mob.SummonedWolf;
import com.eternity.races.common.network.RacesNetwork;
import com.eternity.races.common.progression.ProgressionLoader;
import com.eternity.races.common.registry.ModAttachments;
import com.eternity.races.common.registry.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(RacesMod.MOD_ID)
public class RacesMod {

    public static final String MOD_ID = "racecraft";
    public static final String MOD_NAME = "EternityRaces";

    public RacesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerEntityAttributes);

        ModItems.ITEMS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEntities.register(modEventBus);
        RacesNetwork.register(modEventBus);

        NeoForge.EVENT_BUS.register(RaceEventHandler.class);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        modContainer.registerConfig(ModConfig.Type.COMMON, RacesConfig.SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ClientSetup::init);
            modEventBus.addListener(ClientSetup::onRegisterKeyMappings);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Server-side common setup
        });
    }

    /**
     * Регистрирует атрибуты всех призванных мобов.
     * Должно вызываться ДО загрузки мира.
     */
    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_WOLF.get(), SummonedWolf.createAttributes().build());
        event.put(ModEntities.SUMMONED_SPIDER.get(), SummonedSpider.createAttributes().build());
        event.put(ModEntities.SUMMONED_SLIME.get(), SummonedSlime.createAttributes().build());
    }

    /**
     * Регистрирует ProgressionLoader как resource reload listener.
     * Вызывается каждый раз при /reload и при старте мира.
     */
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ProgressionLoader.INSTANCE);
    }
}
