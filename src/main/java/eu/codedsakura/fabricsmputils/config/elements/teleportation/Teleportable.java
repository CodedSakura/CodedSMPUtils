package eu.codedsakura.fabricsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.fabricsmputils.config.elements.Locale;

import java.util.ArrayList;

public abstract class Teleportable {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("stand-still") public int standStill = 5;
    @Property public int cooldown = 30;

    @Property("boss-bar") public String bossBar = null; // null if off
    @Property("action-bar") public Boolean actionBar = null;
    @Property("allow-back") public Boolean allowBack = null;
}
