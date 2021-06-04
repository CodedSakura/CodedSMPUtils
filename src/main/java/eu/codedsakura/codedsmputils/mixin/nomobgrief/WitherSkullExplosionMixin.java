package eu.codedsakura.codedsmputils.mixin.nomobgrief;

import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WitherSkullEntity.class)
public class WitherSkullExplosionMixin {
    @ModifyVariable(method = "onCollision", at = @At("STORE"), ordinal = 0)
    private Explosion.DestructionType disableDamage(Explosion.DestructionType ignored) {
        return Explosion.DestructionType.NONE;
    }
}
