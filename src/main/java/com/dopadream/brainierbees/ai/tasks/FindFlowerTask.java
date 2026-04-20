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

import java.util.List;
import java.util.Map;

public class FindFlowerTask extends Behavior<Bee> {

    public FindFlowerTask() {
        super(Map.of(
                ModMemoryTypes.POLLINATING_COOLDOWN, MemoryStatus.VALUE_ABSENT,
                ModMemoryTypes.FLOWER_POS, MemoryStatus.VALUE_ABSENT // Only start if we don't have a target
        ));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Bee entity) {
        // Only look for a flower if they don't have nectar and aren't wanting a hive
        boolean wantsHive = entity.getBrain().getMemory(ModMemoryTypes.WANTS_HIVE).orElse(false);
        return !entity.hasNectar() && !wantsHive;
    }

    @Override
    protected void start(ServerLevel level, Bee bee, long l) {
        BlockPos foundFlowerPos = findNearbyFlower(level, bee);

        if (foundFlowerPos != null) {
            // Set the memory immediately so tick() and canStillUse() work
            bee.getBrain().setMemory(ModMemoryTypes.FLOWER_POS, GlobalPos.of(level.dimension(), foundFlowerPos));
        } else {
            // If no flower found, set cooldown so we don't lag the server scanning every tick
            bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, UniformInt.of(120, 240).sample(level.getRandom()));
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Bee bee, long l) {
        return bee.getBrain().hasMemoryValue(ModMemoryTypes.FLOWER_POS) && !bee.hasNectar();
    }

    @Override
    protected void tick(ServerLevel level, Bee bee, long l) {
        bee.getBrain().getMemory(ModMemoryTypes.FLOWER_POS).ifPresent(globalPos -> {
            BlockPos pos = globalPos.pos();

            // 1. Move and Look
            BehaviorUtils.setWalkAndLookTargetMemories(bee, pos, 0.4F, 1);

            // 2. Check reachability
            Path path = bee.getNavigation().createPath(pos, 1);
            if (path != null && path.canReach()) {
                bee.getNavigation().moveTo(path, 0.6);

                // 3. Arrival Logic
                if (bee.blockPosition().closerThan(pos, 2)) {
                    // Bee has arrived! The PollinateTask (if you have one) should take over now.
                    bee.getNavigation().stop();
                }
            } else {
                // Path blocked or impossible
                bee.getBrain().eraseMemory(ModMemoryTypes.FLOWER_POS);
                bee.getBrain().setMemory(ModMemoryTypes.POLLINATING_COOLDOWN, 100);
            }
        });
    }

    private BlockPos findNearbyFlower(ServerLevel level, Bee bee) {
        int radius = BrainierBees.FLOWER_LOCATE_RANGE;
        List<BlockPos> possibles = new java.util.ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                bee.blockPosition().offset(-radius, -radius, -radius),
                bee.blockPosition().offset(radius, radius, radius))) {

            if (level.getBlockState(pos).is(BlockTags.FLOWERS)) {
                // Basic waterlog check (Optional, depending on your needs)
                possibles.add(pos.relative(net.minecraft.core.Direction.UP).below());
            }
        }

        return possibles.isEmpty() ? null : possibles.get(level.random.nextInt(possibles.size()));
    }
}