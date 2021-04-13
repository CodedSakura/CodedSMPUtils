package eu.codedsakura.fabricsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.Property;

public class Spawn extends Teleportable {
    @Property("stand-still") public int standStill = 3;
    @Property public int cooldown = 120;
}
