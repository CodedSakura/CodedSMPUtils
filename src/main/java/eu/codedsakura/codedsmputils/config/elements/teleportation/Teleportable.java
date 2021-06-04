package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.codedsmputils.config.elements.Locale;

import java.util.ArrayList;

public abstract class Teleportable {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("stand-still") public int standStill = 5;
    @Property public int cooldown = 30;

    @Property("boss-bar") public String bossBar = "purple"; // null if off
    @Property("action-bar") public boolean actionBar = false;
    @Property("allow-back") public boolean allowBack = true;
}
