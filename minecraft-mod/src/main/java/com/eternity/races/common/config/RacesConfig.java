package com.eternity.races.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Конфигурация мода EternityRaces через YACL/ModConfigSpec.
 * Все настройки хранятся в файле racecraft-common.toml.
 */
public class RacesConfig {

    public static final Common COMMON;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = new Common(builder);
        SPEC = builder.build();
    }

    public static class Common {

        // ─── General ─────────────────────────────────────────────────────────

        public final ModConfigSpec.BooleanValue enableRaces;
        public final ModConfigSpec.BooleanValue showWelcomeMessage;
        public final ModConfigSpec.BooleanValue grantBookOnJoin;

        // ─── Balance ─────────────────────────────────────────────────────────

        public final ModConfigSpec.DoubleValue cooldownMultiplier;
        public final ModConfigSpec.DoubleValue damageMultiplier;
        public final ModConfigSpec.DoubleValue pointGainMultiplier;
        public final ModConfigSpec.DoubleValue ringExpMultiplier;

        // ─── Visual ──────────────────────────────────────────────────────────

        public final ModConfigSpec.BooleanValue showActionbarMessages;
        public final ModConfigSpec.BooleanValue showCooldownOverlay;

        // ─── Server ──────────────────────────────────────────────────────────

        public final ModConfigSpec.BooleanValue allowReset;
        public final ModConfigSpec.BooleanValue resetCostEnabled;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("general");
            enableRaces = builder
                    .comment("Включить/отключить весь мод EternityRaces")
                    .define("enableRaces", true);
            showWelcomeMessage = builder
                    .comment("Показывать приветственное сообщение при первом входе")
                    .define("showWelcomeMessage", true);
            grantBookOnJoin = builder
                    .comment("Выдавать книгу «Том рас» при первом входе")
                    .define("grantBookOnJoin", true);
            builder.pop();

            builder.push("balance");
            cooldownMultiplier = builder
                    .comment("Глобальный множитель кулдауна способностей (0.5 = вдвое быстрее, 2.0 = вдвое медленнее)")
                    .defineInRange("cooldownMultiplier", 1.0, 0.1, 5.0);
            damageMultiplier = builder
                    .comment("Глобальный множитель урона способностей")
                    .defineInRange("damageMultiplier", 1.0, 0.1, 5.0);
            pointGainMultiplier = builder
                    .comment("Глобальный множитель получения очков рас")
                    .defineInRange("pointGainMultiplier", 1.0, 0.1, 10.0);
            ringExpMultiplier = builder
                    .comment("Глобальный множитель опыта кольца")
                    .defineInRange("ringExpMultiplier", 1.0, 0.1, 10.0);
            builder.pop();

            builder.push("visual");
            showActionbarMessages = builder
                    .comment("Показывать сообщения об активации способностей в ActionBar")
                    .define("showActionbarMessages", true);
            showCooldownOverlay = builder
                    .comment("Показывать оверлей кулдаунов на хотбаре")
                    .define("showCooldownOverlay", true);
            builder.pop();

            builder.push("server");
            allowReset = builder
                    .comment("Разрешить игрокам сбрасывать потраченные очки")
                    .define("allowReset", true);
            resetCostEnabled = builder
                    .comment("Требовать ресурсы для сброса очков")
                    .define("resetCostEnabled", true);
            builder.pop();
        }
    }
}
