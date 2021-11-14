package eu.codedsakura.codedsmputils.modules.bottle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.common.ExperienceUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.L;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Bottle {
    public final static String KEY = "csmpu:points";

    public Bottle(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("bottle")
                .requires(Permissions.require("codedsmputils.bottle", true)
                        .and(source -> CONFIG.bottle != null))
                .then(argument("amount", IntegerArgumentType.integer(0))
                        .then(literal("points")
                                .executes(this::bottlePoints))
                        .then(literal("levels")
                                .executes(this::bottleLevels)))
                .executes(this::bottleAll));
        dispatcher.register(literal("bottle-utils")
                .requires(source -> CONFIG.bottle != null)
                .then(literal("convert")
                        .then(argument("amount", IntegerArgumentType.integer(0))
                                .then(literal("lvlToPoints")
                                        .executes(this::levelsToTotalPoints)))));
    }

    private static NbtList getLore(int amount, long current) {
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serializer.toJson(
                L.get("bottle.description.line-1", new HashMap<String, Object>() {{
                    put("points", amount);
                    put("current_points", current);
                    put("resulting_points", amount + current);
                }}).shallowCopy().styled(style -> style.withItalic(false)))
        ));
        lore.add(NbtString.of(Text.Serializer.toJson(
                L.get("bottle.description.line-2", new HashMap<String, Object>() {{
                    put("points", amount);
                    put("current_points", current);
                    put("resulting_points", amount + current);
                }}).shallowCopy().styled(style -> style.withItalic(false)))
        ));
        return lore;
    }

    public static void rewriteLore(ServerPlayerEntity player, NbtCompound bottleNbt) {
        int amount = bottleNbt.getInt(KEY);
        NbtCompound display = bottleNbt.getCompound(ItemStack.DISPLAY_KEY);
        display.put(ItemStack.LORE_KEY, getLore(amount, ExperienceUtils.playerXPToPoints(player)));
    }

    private int bottleAll(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        int amount = (int) ExperienceUtils.playerXPToPoints(player);
        if (amount <= 0) {
            player.sendMessage(L.get("bottle.insufficient.points"), false);
            return 1;
        }

        createBottle(player, amount);
        return 1;
    }

    private int levelsToTotalPoints(CommandContext<ServerCommandSource> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ctx.getSource().sendFeedback(L.get("bottle.levels-to-points", new HashMap<String, Object>() {{
            put("levels", amount);
            put("points", ExperienceUtils.levelToTotalPoints(amount));
        }}), false);
        return 0;
    }

    private int bottleLevels(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        int amount = (int) ExperienceUtils.levelToTotalPoints(IntegerArgumentType.getInteger(ctx, "amount"));

        if (ExperienceUtils.playerXPToPoints(player) < amount) {
            player.sendMessage(L.get("bottle.insufficient.levels"), false);
            return 1;
        }

        createBottle(player, amount);
        return 1;
    }

    private int bottlePoints(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        if (ExperienceUtils.playerXPToPoints(player) < amount) {
            player.sendMessage(L.get("bottle.insufficient.points"), false);
            return 1;
        }

        createBottle(player, amount);
        return 1;
    }

    private void createBottle(ServerPlayerEntity player, int amount) {
        ItemStack bottle = new ItemStack(Items.EXPERIENCE_BOTTLE, 1);
        NbtCompound bottleData = new NbtCompound();
        bottleData.putInt(KEY, amount);
        NbtCompound display = new NbtCompound();
        display.put(ItemStack.NAME_KEY, NbtString.of(Text.Serializer.toJson(
                L.get("bottle.title").shallowCopy().styled(style -> style.withItalic(false))
        )));
        display.put(ItemStack.LORE_KEY, getLore(amount, ExperienceUtils.playerXPToPoints(player)));
        bottleData.put(ItemStack.DISPLAY_KEY, display);
        bottle.setNbt(bottleData);
        player.giveItemStack(bottle);
        player.addExperience(-amount);
    }
}
