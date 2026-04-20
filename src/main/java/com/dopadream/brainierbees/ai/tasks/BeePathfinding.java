package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.BrainierBees;
import com.dopadream.brainierbees.ai.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.Optional;

public class BeePathfinding extends Behavior<Bee> {

    public BeePathfinding() {
        super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    // Make bees not get stuck on ceiling anymore and lag people as a result.
    // Original code by TelepathicGrunt, edited and repurposed by dopadream with permission!
    // Check out Bumblezone!

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Bee bee) {
        return (bee.getNavigation().isDone() && bee.getRandom().nextInt(10) == 0) || !bee.getBrain().hasMemoryValue(ModMemoryTypes.HIVE_POS);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee bee, long l) {
        return bee.getNavigation().isInProgress() || !bee.getBrain().hasMemoryValue(ModMemoryTypes.HIVE_POS);
    }

    @Override
    protected void start(ServerLevel serverLevel, Bee bee, long l) {
        // Execute the smart pathfinding logic directly
        smartBeesPathfind(bee);
    }

    @Override
    protected void tick(ServerLevel serverLevel, Bee bee, long l) {
        super.tick(serverLevel, bee, l);
        if (bee.hasNectar()) {
            bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, 400);
        }
    }

    private static void smartBeesPathfind(Bee bee) {
        Level world = bee.level();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(bee.blockPosition());
        LevelChunk levelChunk = world.getChunkAt(mutable);
        int height = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, mutable.getX(), mutable.getZ()) + 1;

        // Safely grab the hive pos memory once
        Optional<GlobalPos> hivePosOpt = bee.getBrain().getMemory(ModMemoryTypes.HIVE_POS);

        for (int attempt = 0; attempt < 11 || bee.blockPosition().distManhattan(mutable) <= 5; attempt++) {
            // pick a random place to fly to
            if ((world.dimensionType().hasCeiling()) || (bee.getBlockY() <= (height + 3))) {
                mutable.set(bee.blockPosition()).move(
                        bee.getRandom().nextInt(21) - 10,
                        bee.getRandom().nextInt(6) - 2,
                        bee.getRandom().nextInt(21) - 10
                );
            } else {
                mutable.set(bee.blockPosition()).move(
                        bee.getRandom().nextInt(21) - 10,
                        bee.getRandom().nextInt(6) - 5,
                        bee.getRandom().nextInt(21) - 10
                );
            }

            // Check if the block 2 blocks below the target is air
            boolean isAirBelow = world.getBlockState(mutable.below(2)).isAir();

            if (hivePosOpt.isEmpty()) {
                if (isAirBelow) {
                    break; // Valid spot to go towards. Homeless bees only!
                }
            } else {
                // Statically access MAX_WANDER_RADIUS and cleanly check distance
                if (mutable.closerThan(hivePosOpt.get().pos(), BrainierBees.MAX_WANDER_RADIUS) && isAirBelow) {
                    break; // Valid spot to go towards within a set radius of their home
                }
            }
        }

        Path newPath = bee.getNavigation().createPath(mutable, 1);
        if (newPath != null) {
            bee.getNavigation().moveTo(newPath, 1);
        }
    }
}