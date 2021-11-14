package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.modules.bottle.Bottle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceBottleItem.class)
public class XPBottleMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        NbtCompound nbt = itemStack.getNbt();
        if (nbt != null && nbt.contains(Bottle.KEY)) {
            int amount = nbt.getInt(Bottle.KEY);
            user.addExperience(amount);
            itemStack.decrement(1);
            cir.setReturnValue(
                    TypedActionResult.success(itemStack, world.isClient)
            );
        }
    }
}
