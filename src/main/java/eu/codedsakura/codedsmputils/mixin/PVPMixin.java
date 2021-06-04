package eu.codedsakura.codedsmputils.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static eu.codedsakura.codedsmputils.SMPUtilCardinalComponents.PVP_DATA;

@Mixin(ServerPlayerEntity.class)
public class PVPMixin {
    @Inject(method = "shouldDamagePlayer", at = @At("HEAD"), cancellable = true)
    private void testDamagePlayer(PlayerEntity player, CallbackInfoReturnable<Boolean> ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        ci.setReturnValue(PVP_DATA.get(self).isOn() && PVP_DATA.get(player).isOn());

    }
}
