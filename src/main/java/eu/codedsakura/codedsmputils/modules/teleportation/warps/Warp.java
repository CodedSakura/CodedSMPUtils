package eu.codedsakura.codedsmputils.modules.teleportation.warps;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.UUID;

public class Warp {
    public double x, y, z;
    public float yaw, pitch;
    public String name;
    public UUID id, owner;

    public Warp(double x, double y, double z, float yaw, float pitch, String name, UUID owner, UUID id) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.name = name;
        this.owner = owner;
        this.id = id;
    }

    public Warp(double x, double y, double z, float yaw, float pitch, String name, UUID owner) {
        this(x, y, z, pitch, yaw, name, owner, UUID.randomUUID());
    }

    public Warp(Vec3d pos, Vec2f rot, String name, UUID owner) {
        this(pos.x, pos.y, pos.z, rot.x, rot.y, name, owner);
    }

    public HashMap<String, ?> asArguments(ServerWorld world) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(this.owner);
        return new HashMap<String, Object>() {{
            put("name", name);
            put("author", player == null ? "[unknown]" : player.getName().asString());
            put("x", x);
            put("y", y);
            put("z", z);
            put("yaw", yaw);
            put("pitch", pitch);
            put("owner_uuid", owner);
            put("dimension", world.getRegistryKey().getValue().toString());
            put("id", id.toString());
            put("id_trunc", id.toString().substring(0, 21) + "...");
        }};
    }

    @Override
    public String toString() {
        return "Warp{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", owner=" + owner +
                ", id=" + id +
                '}';
    }
}
