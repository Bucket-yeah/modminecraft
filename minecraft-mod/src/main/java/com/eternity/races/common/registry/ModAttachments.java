package com.eternity.races.common.registry;

import com.eternity.races.RacesMod;
import com.eternity.races.common.cap.RaceData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RacesMod.MOD_ID);

    /**
     * Хранит данные расы игрока: ID расы, очки, способности, кулдауны.
     */
    public static final Supplier<AttachmentType<RaceData>> RACE_DATA =
            ATTACHMENT_TYPES.register("race_data",
                    () -> AttachmentType.builder(RaceData::new)
                            .serialize(RaceData.CODEC)
                            .copyOnDeath()
                            .build());
}
