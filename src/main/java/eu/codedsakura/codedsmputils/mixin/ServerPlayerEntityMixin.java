package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.modules.bottle.Bottle;
import eu.codedsakura.codedsmputils.modules.teleportation.lastdeath.LastDeath;
import eu.codedsakura.common.ExperienceUtils;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
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


    @Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExperienceBarUpdateS2CPacket;<init>(FII)V"))
    private void playerTickXPChange(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        for (int i = 0; i < self.getInventory().size(); i++) {
            ItemStack stack = self.getInventory().getStack(i);

            if (!stack.isOf(Items.EXPERIENCE_BOTTLE)) continue;
            if (!stack.hasNbt()) continue;
            assert stack.getNbt() != null;
            if (!stack.getNbt().contains(Bottle.KEY)) continue;

            NbtCompound bottleNbt = stack.getNbt();

            Bottle.rewriteLore(self, bottleNbt);

            stack.setNbt(bottleNbt);
        }
    }
}
