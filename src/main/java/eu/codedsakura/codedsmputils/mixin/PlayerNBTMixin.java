package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.modules.pvp.PvPComponent;
import eu.codedsakura.codedsmputils.modules.teleportation.homes.HomeDataComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerNBTMixin {
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        HomeDataComponent.writeToNbt(nbt, self.getUuid());
        PvPComponent.writeToNbt(nbt, self.getUuid());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        HomeDataComponent.readFromNbt(nbt, self.getUuid());
        PvPComponent.readFromNbt(nbt, self.getUuid());
    }
}
