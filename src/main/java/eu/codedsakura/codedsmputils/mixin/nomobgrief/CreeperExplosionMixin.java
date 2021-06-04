package eu.codedsakura.codedsmputils.mixin.nomobgrief;

import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CreeperEntity.class)
public class CreeperExplosionMixin {
    @ModifyVariable(method = "explode", at = @At("STORE"), ordinal = 0)
    private Explosion.DestructionType disableDamage(Explosion.DestructionType ignored) {
        return Explosion.DestructionType.NONE;
    }
}
