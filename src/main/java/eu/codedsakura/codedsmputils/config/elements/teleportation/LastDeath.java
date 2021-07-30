package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.Property;

public class LastDeath extends Teleportable {
    @Property("allow-back")
    public boolean allowBack = false;
    @Property
    public int cooldown = 300;
}
