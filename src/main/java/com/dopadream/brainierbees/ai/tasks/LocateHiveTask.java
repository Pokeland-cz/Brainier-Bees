package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.ai.ModMemoryTypes;
import com.dopadream.brainierbees.util.HiveAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocateHiveTask extends Behavior<Bee> {

    public LocateHiveTask() {
        // Optimally, declare all memory requirements here so the Brain skips execution immediately if conditions fail.
        super(Map.of(
                ModMemoryTypes.COOLDOWN_LOCATE_HIVE, MemoryStatus.VALUE_ABSENT,
                ModMemoryTypes.HIVE_POS, MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee livingEntity, long l) {
        return false;
    }

    @Override
    protected void start(ServerLevel level, Bee bee, long l) {
        bee.getBrain().setMemory(ModMemoryTypes.COOLDOWN_LOCATE_HIVE, 200);

        BlockPos currentPos = bee.blockPosition();
        PoiManager poiManager = level.getPoiManager();

        // Fetch the blacklist once rather than repeatedly in a loop
        Optional<List<GlobalPos>> blacklistOpt = bee.getBrain().getMemory(ModMemoryTypes.HIVE_BLACKLIST);

        // Utilize lazy evaluation to find the first valid hive without checking every BlockEntity
        poiManager.getInRange(holder -> holder.is(PoiTypeTags.BEE_HOME), currentPos, 20, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                // 1. Cheap Check: Filter out blacklisted hives first
                .filter(pos -> blacklistOpt.isEmpty() || !blacklistOpt.get().contains(GlobalPos.of(level.dimension(), pos)))
                // 2. Cheap Math: Sort by distance
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(currentPos)))
                // 3. Expensive Check: Filter by capacity last
                .filter(pos -> doesHiveHaveSpace(level, pos))
                // 4. Lazy Execution: Only check until we find the first valid one
                .findFirst()
                .ifPresent(bestHivePos -> {
                    bee.getBrain().setMemory(ModMemoryTypes.HIVE_POS, GlobalPos.of(level.dimension(), bestHivePos));
                    ((HiveAccessor) bee).setMemorizedHome(bestHivePos);
                });
    }

    private boolean doesHiveHaveSpace(ServerLevel level, BlockPos blockPos) {
        // Modern Java instance matching prevents casting boilerplate
        return level.getBlockEntity(blockPos) instanceof BeehiveBlockEntity beehive && !beehive.isFull();
    }
}