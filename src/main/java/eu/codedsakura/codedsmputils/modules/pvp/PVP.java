package eu.codedsakura.codedsmputils.modules.pvp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Collections;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.L;
import static eu.codedsakura.codedsmputils.SMPUtilCardinalComponents.PVP_DATA;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PVP {
    public PVP(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("pvp")
            .requires(source -> CONFIG.pvp != null)
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
    }

    private int setPvP(CommandContext<ServerCommandSource> ctx, boolean b) throws CommandSyntaxException {
        return setPvP(ctx, b, Collections.singletonList(ctx.getSource().getPlayer()));
    }
    private int setPvP(CommandContext<ServerCommandSource> ctx, boolean b, Collection<ServerPlayerEntity> players) {
        players.forEach(v -> PVP_DATA.get(v).set(b));

        ctx.getSource().sendFeedback(
                L.get(String.format("pvp.change.%s.%s", players.size() == 1 ? "one" : "all", b ? "on" : "off")),
                players.size() > 1
        );
        return 1;
    }

    private int togglePvP(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        IPvPComponent pvpComponent = PVP_DATA.get(ctx.getSource().getPlayer());
        pvpComponent.set(!pvpComponent.isOn());

        ctx.getSource().sendFeedback(
                L.get(String.format("pvp.change.one.%s", pvpComponent.isOn() ? "on" : "off")),
                false
        );
        return 1;
    }
}
