package com.eternity.races.common.network;

import com.eternity.races.RacesMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class RacesNetwork {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RacesNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RacesMod.MOD_ID).versioned("1.0.0").optional();

        // Сервер → Клиент: синхронизация данных расы
        registrar.playToClient(
                SyncRaceDataPacket.TYPE,
                SyncRaceDataPacket.STREAM_CODEC,
                SyncRaceDataPacket::handle
        );

        // Клиент → Сервер: выбор расы
        registrar.playToServer(
                SelectRacePacket.TYPE,
                SelectRacePacket.STREAM_CODEC,
                SelectRacePacket::handle
        );

        // Клиент → Сервер: активация способности
        registrar.playToServer(
                AbilityActivationPacket.TYPE,
                AbilityActivationPacket.STREAM_CODEC,
                AbilityActivationPacket::handle
        );

        // Клиент → Сервер: разблокировка способности в дереве
        registrar.playToServer(
                UnlockAbilityPacket.TYPE,
                UnlockAbilityPacket.STREAM_CODEC,
                UnlockAbilityPacket::handle
        );

        // Клиент → Сервер: сброс очков
        registrar.playToServer(
                ResetPointsPacket.TYPE,
                ResetPointsPacket.STREAM_CODEC,
                ResetPointsPacket::handle
        );
    }
}
