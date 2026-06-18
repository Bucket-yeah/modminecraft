package com.eternity.races.common.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

import java.util.List;
import java.util.UUID;

/**
 * Базовый класс для всех призванных существ.
 * Использует SmartBrainLib для AI: следует за хозяином, атакует его врагов.
 */
public abstract class SummonedMob extends Mob implements SmartBrainOwner<SummonedMob> {

    private UUID ownerUUID;
    private int lifetimeTicks;
    private int maxLifetimeTicks;

    protected SummonedMob(EntityType<? extends SummonedMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ─── SmartBrainLib ───────────────────────────────────────────────────────

    @Override
    protected SmartBrainProvider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    public List<? extends ExtendedSensor<? extends SummonedMob>> getSensors() {
        return List.of(
                new NearbyPlayersSensor<>(),
                new NearbyLivingEntitySensor<>(),
                new HurtBySensor<>()
        );
    }

    @Override
    public BrainActivityGroup<? extends SummonedMob> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new FollowOwner<>(this::getOwner, 1.2f, 16, 3),
                new MoveToWalkTarget<>()
        );
    }

    @Override
    public BrainActivityGroup<? extends SummonedMob> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<>(
                        new TargetOrRetaliate<>(),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()
                ),
                new OneRandomBehaviour<>(
                        new SetRandomWalkTarget<>(),
                        new Idle<>().runFor(e -> e.getRandom().nextIntBetweenInclusive(30, 60))
                )
        );
    }

    @Override
    public BrainActivityGroup<? extends SummonedMob> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new AnimatableMeleeAttack<>(20)
        );
    }

    // ─── Хозяин и время жизни ────────────────────────────────────────────────

    public void setOwner(Player owner) {
        this.ownerUUID = owner.getUUID();
    }

    public Player getOwner() {
        if (ownerUUID == null || level().isClientSide) return null;
        return level().getServer().getPlayerList().getPlayer(ownerUUID);
    }

    public void setLifetime(int ticks) {
        this.maxLifetimeTicks = ticks;
        this.lifetimeTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            lifetimeTicks++;
            if (maxLifetimeTicks > 0 && lifetimeTicks >= maxLifetimeTicks) {
                this.discard();
            }
        }
    }
}
