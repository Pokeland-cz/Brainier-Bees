package com.dopadream.brainierbees.ai.tasks;

import com.dopadream.brainierbees.BrainierBees;
import com.dopadream.brainierbees.ai.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.pathfinder.Path;

import java.util.Map;
import java.util.Optional;

public class FindFlowerTask extends Behavior<Bee> {

    // REMOVED: private BlockPos flowerPosPublic; (Prevents the Hive Mind bug!)

    public FindFlowerTask() {
        super(Map.of(
                ModMemoryTypes.POLLINATING_COOLDOWN, MemoryStatus.VALUE_ABSENT
                // If you want to require FLOWER_POS to be absent to start searching, add it here:
                // ModMemoryTypes.FLOWER_POS, MemoryStatus.VALUE_ABSENT
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Bee entity) {
        // Only look for a flower if they don't have nectar and aren't on cooldown
        return !entity.hasNectar() && entity.getBrain().getMemory(ModMemoryTypes.POLLINATING_COOLDOWN).isEmpty();
    }

    @Override
    protected void start(ServerLevel serverLevel, Bee bee, long l) {
        // Vanilla bee flower finding logic is usually handled by a sensor or POI search.
        // Assuming your custom logic finds a BlockPos here, immediately save it to the Bee's brain:

        BlockPos foundFlowerPos = findNearbyFlower(serverLevel, bee); // Replace with your actual search logic

        if (foundFlowerPos != null) {
            bee.getBrain().setMemory(ModMemoryTypes.FLOWER_POS, GlobalPos.of(serverLevel.dimension(), foundFlowerPos));
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee bee, long l) {
        return bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS).isPresent();
    }

    @Override
    protected void tick(ServerLevel level, Bee bee, long l) {
        Optional<GlobalPos> flowerPosOpt = bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS);

        if (flowerPosOpt.isPresent()) {
            BlockPos flowerPos = flowerPosOpt.get().pos();
            BehaviorUtils.setWalkAndLookTargetMemories(bee, flowerPos, 0.4F, 1);

            Path path = bee.getNavigation().createPath(flowerPos, 1);
            if (path != null && path.canReach()) {
                bee.getNavigation().moveTo(path, 0.6);

                // If they reached the flower, we can transition to pollinating
                if (bee.blockPosition().closerThan(flowerPos, 2) && level.getBlockState(flowerPos).is(BlockTags.FLOWERS)) {
                    // Logic to start pollinating or stop moving
                    bee.getNavigation().stop();
                }
            } else {
                // Cannot reach flower, clear memory and apply cooldown
                bee.getBrain().eraseMemory(ModMemoryTypes.FLOWER_POS);
                bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, UniformInt.of(120, 240).sample(level.getRandom()));
            }
        }
    }

    @Override
    protected void stop(ServerLevel serverLevel, Bee bee, long l) {
        super.stop(serverLevel, bee, l);
        // Any stopping logic you had
    }

    // Stub for your actual search logic
    private BlockPos findNearbyFlower(ServerLevel level, Bee bee) {
        // Your logic to scan blocks around the bee within BrainierBees.FLOWER_LOCATE_RANGE
        return null;
    }
}