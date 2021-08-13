package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.Property;

public class Spawn extends Teleportable {
    @Property("use-radius")
    public boolean useRadius = false;
    @Property("search-up")
    public boolean searchUp = true;
    @Property("search-down")
    public boolean searchDown = true;

    @Property
    public String world = "minecraft:overworld";

    @Property("stand-still")
    public int standStill = 3;
    @Property
    public int cooldown = 120;
}
