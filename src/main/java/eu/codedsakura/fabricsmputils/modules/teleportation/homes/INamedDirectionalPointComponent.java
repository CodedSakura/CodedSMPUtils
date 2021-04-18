package eu.codedsakura.fabricsmputils.modules.teleportation.homes;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public interface INamedDirectionalPointComponent {
    double getX();
    double getY();
    double geyZ();
    float getPitch();
    float getYaw();
    String getName();
    Vec3d getCoords();
    Identifier getDimID();
}
