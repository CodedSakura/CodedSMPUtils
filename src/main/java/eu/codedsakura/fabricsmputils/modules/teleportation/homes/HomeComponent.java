package eu.codedsakura.fabricsmputils.modules.teleportation.homes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public class HomeComponent implements INamedDirectionalPointComponent {
    private final double x, y, z;
    private final float pitch, yaw;
    private final String name;
    private final Identifier dim;

    public HomeComponent(double x, double y, double z, float pitch, float yaw, Identifier dim, String name) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.name = name;
        this.dim = dim;
    }

    public HomeComponent(Vec3d pos, float pitch, float yaw, Identifier dim, String name) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.name = name;
        this.dim = dim;
    }

    public static HomeComponent readFromNbt(CompoundTag tag) {
        return new HomeComponent(
            tag.getDouble("x"),
            tag.getDouble("y"),
            tag.getDouble("z"),
            tag.getFloat("pitch"),
            tag.getFloat("yaw"),
            Identifier.tryParse(tag.getString("dim")),
            tag.getString("name")
        );
    }

    public void writeToNbt(CompoundTag tag) {
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("pitch", pitch);
        tag.putFloat("yaw", yaw);
        tag.putString("name", name);
        tag.putString("dim", dim.toString());
    }

    @Override public double getX()  { return x; }
    @Override public double getY()  { return y; }
    @Override public double geyZ()  { return z; }
    @Override public float getPitch()  { return pitch; }
    @Override public float getYaw()    { return yaw;   }
    @Override public String getName()   { return name;  }
    @Override public Vec3d getCoords()  { return new Vec3d(x, y, z); }
    @Override public Identifier getDimID() { return dim; }

    public Map<String, ?> asArguments() {
        return new HashMap<String, Object>() {{
            put("name", name);
            put("x", x);
            put("y", y);
            put("z", z);
            put("yaw", yaw);
            put("pitch", pitch);
            put("dimension", dim.toString());
        }};
    }
}
