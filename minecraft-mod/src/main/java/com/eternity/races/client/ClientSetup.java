package com.eternity.races.client;

import com.eternity.races.client.keybind.KeyBindings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Клиентская инициализация: регистрация клавиш, оверлеев, рендереров.
 */
public class ClientSetup {

    /**
     * Вызывается из RacesMod через modEventBus.addListener(ClientSetup::init).
     * Здесь регистрируем события, которые нужны только на клиенте.
     */
    public static void init(FMLClientSetupEvent event) {
        // Регистрируем слушатель клиентских тиков (нажатия клавиш)
        NeoForge.EVENT_BUS.register(KeyBindings.class);
    }

    /**
     * Регистрация клавиш — вызывается из modEventBus в RacesMod.
     */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyBindings.registerAll(event);
    }
}
