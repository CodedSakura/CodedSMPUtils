package eu.codedsakura.codedsmputils.mixin.nomobgrief;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = {"net.minecraft.entity.mob.EndermanEntity$PickUpBlockGoal", "net.minecraft.entity.mob.EndermanEntity$PlaceBlockGoal"})
public interface PickUpBlockGoalDummy { }
