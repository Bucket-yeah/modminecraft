package com.eternity.races.common.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Призванный слизень Химеры-Симбионта (способность «Размножение»).
 * Призывается 3 штуки, живёт 20 секунд (400 тиков).
 */
public class SummonedSlime extends SummonedMob {

    public SummonedSlime(EntityType<? extends SummonedSlime> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public void tick() {
        super.tick();
        // Взрываемся по истечении срока (обрабатывается в SummonedMob через setLifetime)
    }
}
