package eu.codedsakura.codedsmputils.mixin.nomobgrief;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EndermanEntity.class)
public class EndermanBlockPickupMixin extends HostileEntity {
    protected EndermanBlockPickupMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void removeSomeGoals(CallbackInfo ci) {
        Set<PrioritizedGoal> goals = ((GoalSelectorAccessor) this.goalSelector).getGoals();
        goals.removeIf(v -> v.getGoal() instanceof PickUpBlockGoalDummy);
    }
}
