package eu.codedsakura.fabricsmputils.modules.pvp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.config;
import static eu.codedsakura.fabricsmputils.FabricSMPUtils.l;
import static eu.codedsakura.fabricsmputils.SMPUtilCardinalComponents.PVP_DATA;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PVP {
    public void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, listener) -> {
            dispatcher.register(literal("pvp")
                    .requires(source -> config.pvp != null)
                    .requires(Permissions.require("fabricspmutils.pvp", true))
                    .executes(this::togglePvP)
                    .then(literal("on")
                            .executes(ctx -> this.setPvP(ctx, true))
                            .then(argument("players", EntityArgumentType.players())
                                    .requires(Permissions.require("fabricspmutils.pvp.others", 2))
                                    .executes(ctx -> this.setPvP(ctx, true, EntityArgumentType.getPlayers(ctx, "players")))))
                    .then(literal("off")
                            .executes(ctx -> this.setPvP(ctx, false))
                            .then(argument("players", EntityArgumentType.players())
                                    .requires(Permissions.require("fabricspmutils.pvp.others", 2))
                                    .executes(ctx -> this.setPvP(ctx, false, EntityArgumentType.getPlayers(ctx, "players"))))));
        });
    }

    private int setPvP(CommandContext<ServerCommandSource> ctx, boolean b) throws CommandSyntaxException {
        return setPvP(ctx, b, Collections.singletonList(ctx.getSource().getPlayer()));
    }
    private int setPvP(CommandContext<ServerCommandSource> ctx, boolean b, Collection<ServerPlayerEntity> players) {
        players.forEach(v -> PVP_DATA.get(v).set(b));

        String state = l.getText(b ? "pvp.state.on" : "pvp.state.off").asString();

        ctx.getSource().sendFeedback(
                l.getText(players.size() == 1 ? "pvp.change.one" : "pvp.change.all", new HashMap<String, String>() {{put("state", state);}}),
                players.size() > 1
        );
        return 1;
    }

    private int togglePvP(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        IPvPComponent pvpComponent = PVP_DATA.get(ctx.getSource().getPlayer());
        pvpComponent.set(!pvpComponent.isOn());

        String state = l.getText(pvpComponent.isOn() ? "pvp.state.on" : "pvp.state.off").asString();

        ctx.getSource().sendFeedback(
                l.getText("pvp.change.one", new HashMap<String, String>() {{put("state", state);}}),
                false
        );
        return 1;
    }
}
