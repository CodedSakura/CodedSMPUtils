package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.modules.pvp.PvPComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class PVPMixin {
    @Inject(method = "shouldDamagePlayer", at = @At("HEAD"), cancellable = true)
    private void testDamagePlayer(PlayerEntity player, CallbackInfoReturnable<Boolean> ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        if (!PvPComponent.get(self).isOn() || !PvPComponent.get(player).isOn())
            ci.setReturnValue(false);
    }
}
