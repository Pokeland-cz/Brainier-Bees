package com.dopadream.brainierbees.ai.tasks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.Bee;

import java.util.Map;

public class FloatTask extends Behavior<Bee> {

    public FloatTask() {
        // Floating is a survival instinct! It requires no specific memories to trigger.
        super(Map.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel serverLevel, Bee bee) {
        // Added parentheses for better readability of the logic
        return (bee.isInWater() && bee.getFluidHeight(FluidTags.WATER) > bee.getFluidJumpThreshold()) || bee.isInLava();
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, Bee bee, long l) {
        // Keep using this task as long as they are still in the water/lava
        return this.checkExtraStartConditions(serverLevel, bee);
    }

    @Override
    protected void tick(ServerLevel serverLevel, Bee bee, long l) {
        // Constantly jump while ticking to stay afloat (Vanilla Swim behavior does exactly this)
        if (bee.getRandom().nextFloat() < 0.8F) {
            bee.getJumpControl().jump();
        }
    }
}