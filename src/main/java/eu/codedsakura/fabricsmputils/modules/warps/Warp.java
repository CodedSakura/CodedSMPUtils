package eu.codedsakura.fabricsmputils.modules.warps;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

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

    private static MutableText valueRepr(String name, Text value) {
        if (value.getStyle().getColor() == null)
            return new LiteralText(name + ": ").formatted(Formatting.RESET).append(value.copy().formatted(Formatting.GOLD));
        return new LiteralText(name + ": ").formatted(Formatting.RESET).append(value);
    }

    private static MutableText valueRepr(String name, String value) {
        return valueRepr(name, new LiteralText(value).formatted(Formatting.GOLD));
    }

    private static MutableText valueRepr(String name, double value) {
        return valueRepr(name, String.format("%.2f", value));
    }

    private static MutableText valueRepr(String name, float value) {
        return valueRepr(name, String.format("%.2f", value));
    }

    public MutableText toText(ServerWorld world) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(this.owner);
        Text ownerName = player == null ? new LiteralText("[unknown]") : player.getDisplayName();
        return new TranslatableText("%s\n%s\n%s; %s; %s\n%s; %s\n%s\n%s",
                valueRepr("Name", name), valueRepr("Made by", ownerName),
                valueRepr("X", x), valueRepr("Y", y), valueRepr("Z", z),
                valueRepr("Yaw", yaw), valueRepr("Pitch", pitch),
                valueRepr("In", world.getRegistryKey().getValue().toString()),
                valueRepr("ID", id.toString().substring(0, 21) + "..."));
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
