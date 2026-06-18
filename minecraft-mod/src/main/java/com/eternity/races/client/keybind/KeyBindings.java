package com.eternity.races.client.keybind;

import com.eternity.races.RacesMod;
import com.eternity.races.common.network.AbilityActivationPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Клавиши управления мода EternityRaces.
 *
 * G         — открыть дерево способностей
 * R         — способность 1
 * F         — способность 2
 * C         — способность 3
 * V         — способность 4
 * B         — способность 5
 * N         — способность 6
 * M         — способность 7
 * ,         — способность 8
 */
public class KeyBindings {

    public static final String CATEGORY = "key.categories." + RacesMod.MOD_ID;

    public static KeyMapping KEY_OPEN_TREE;
    public static final KeyMapping[] ABILITY_KEYS = new KeyMapping[8];

    private static final int[] DEFAULT_KEYS = {
            GLFW.GLFW_KEY_R,
            GLFW.GLFW_KEY_F,
            GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_V,
            GLFW.GLFW_KEY_B,
            GLFW.GLFW_KEY_N,
            GLFW.GLFW_KEY_M,
            GLFW.GLFW_KEY_COMMA
    };

    public static void registerAll(RegisterKeyMappingsEvent event) {
        KEY_OPEN_TREE = new KeyMapping(
                "key." + RacesMod.MOD_ID + ".open_tree",
                KeyConflictContext.IN_GAME,
                GLFW.GLFW_KEY_G,
                CATEGORY
        );
        event.register(KEY_OPEN_TREE);

        for (int i = 0; i < 8; i++) {
            ABILITY_KEYS[i] = new KeyMapping(
                    "key." + RacesMod.MOD_ID + ".ability_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    DEFAULT_KEYS[i],
                    CATEGORY
            );
            event.register(ABILITY_KEYS[i]);
        }
    }

    /**
     * Проверяет нажатия клавиш на клиенте и отправляет пакеты на сервер.
     * Регистрируется в NeoForge EVENT_BUS на клиенте.
     */
    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (net.minecraft.client.Minecraft.getInstance().player == null) return;
        if (net.minecraft.client.Minecraft.getInstance().screen != null) return;

        // Открытие дерева
        while (KEY_OPEN_TREE != null && KEY_OPEN_TREE.consumeClick()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.eternity.races.client.gui.ProgressionTreeScreen());
        }

        // Активация способностей
        for (int i = 0; i < 8; i++) {
            if (ABILITY_KEYS[i] != null) {
                while (ABILITY_KEYS[i].consumeClick()) {
                    final int idx = i;
                    PacketDistributor.sendToServer(new AbilityActivationPacket(idx));
                }
            }
        }
    }
}
