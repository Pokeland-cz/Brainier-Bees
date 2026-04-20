package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.ai.ModMemoryTypes;
import com.dopadream.brainierbees.mixin.BeeAccessor;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Optional;

public class EnterHiveTask extends Behavior<Bee> {

    public EnterHiveTask() {
        super(Map.of(ModMemoryTypes.HIVE_POS, MemoryStatus.VALUE_PRESENT));
    }

    public boolean wantsToEnterHive(ServerLevel level, Bee bee) {
        if (((BeeAccessor)bee).getStayOutOfHiveCountdown() <= 0 && !bee.hasStung() && bee.getTarget() == null) {
            boolean bl = level.isRaining() || level.isNight() || bee.hasNectar();
            return bl && !this.isHiveNearFire(level, bee);
        } else {
            return false;
        }
    }

    private boolean isHiveNearFire(ServerLevel level, Bee bee) {
        Optional<GlobalPos> hivePosOpt = bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS);
        if (hivePosOpt.isEmpty()) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(hivePosOpt.get().pos());
        return blockEntity instanceof BeehiveBlockEntity beehive && beehive.isFireNearby();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Bee bee) {
        Optional<GlobalPos> hivePosOpt = bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS);
        if (hivePosOpt.isEmpty()) {
            return false;
        }

        GlobalPos hivePos = hivePosOpt.get();

        if (this.wantsToEnterHive(serverLevel, bee) && hivePos.pos().closerToCenterThan(bee.position(), 2.0)) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(hivePos.pos());

            if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
                if (!beehiveBlockEntity.isFull()) {
                    return true;
                }
                // Hive is full, erase memory so they can find a new one
                bee.getBrain().eraseMemory(ModMemoryTypes.HIVE_POS);
            }
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee livingEntity, long l) {
        return false;
    }

    @Override
    protected void start(ServerLevel serverLevel, Bee bee, long l) {
        Optional<GlobalPos> hivePosOpt = bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS);
        if (hivePosOpt.isEmpty()) {
            return;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(hivePosOpt.get().pos());
        if (blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity) {
            beehiveBlockEntity.addOccupant(bee);
        }
    }
}