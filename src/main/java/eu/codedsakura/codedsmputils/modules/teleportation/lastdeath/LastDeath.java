package eu.codedsakura.codedsmputils.modules.teleportation.lastdeath;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import eu.codedsakura.common.TeleportUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.L;
import static net.minecraft.server.command.CommandManager.literal;

public class LastDeath {
    public static HashMap<UUID, DeathPoint> deaths = new HashMap<>();

    public LastDeath(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("lastdeath")
                .requires(Permissions.require("codedsmputils.teleportation.last-death", true)
                        .and(source -> CONFIG.teleportation != null && CONFIG.teleportation.lastDeath != null))
                .executes(this::run));
    }

    private int run(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        //noinspection DuplicatedCode
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        if (TeleportUtils.cantTeleport(player)) return 1;

        if (CooldownManager.check(player, LastDeath.class, "last-death")) return 1;

        if (!LastDeath.deaths.containsKey(player.getUuid())) {
            ctx.getSource().sendFeedback(L.get("teleportation.last-death.no-location"), false);
            return 0;
        }
        DeathPoint loc = LastDeath.deaths.get(player.getUuid());

        //noinspection DuplicatedCode
        TeleportUtils.genericTeleport(
                "teleportation.last-death", CONFIG.teleportation.lastDeath.bossBar, CONFIG.teleportation.lastDeath.actionBar, CONFIG.teleportation.lastDeath.standStill,
                player, () -> {
                    if (CONFIG.teleportation.lastDeath.allowBack) Back.addNewTeleport(player);
                    player.teleport((ServerWorld) loc.world, loc.pos.x, loc.pos.y, loc.pos.z, loc.yaw, loc.pitch);
                    CooldownManager.addCooldown(LastDeath.class, player.getUuid(), CONFIG.teleportation.lastDeath.cooldown);
                });
        return 1;
    }

    public static class DeathPoint {
        public World world;
        public Vec3d pos;
        public float yaw, pitch;
        public long time;

        public DeathPoint(PlayerEntity player, long time) {
            this.world = player.world;
            this.pos = player.getPos();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.time = time;
        }
    }
}
