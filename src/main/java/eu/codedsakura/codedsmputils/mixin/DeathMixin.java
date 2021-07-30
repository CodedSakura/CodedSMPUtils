package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.modules.teleportation.lastdeath.LastDeath;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(ServerPlayerEntity.class)
public class DeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        LastDeath.deaths.put(self.getUuid(), new LastDeath.DeathPoint(self, Instant.now().getEpochSecond()));
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        LastDeath.deaths.remove(self.getUuid());
    }
}
