package eu.codedsakura.codedsmputils.config.elements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class PVP {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("default-state") public boolean defaultState = false; // off = false, on = true
    @Property("stand-still") public int standStill = 3;
    @Property public int cooldown = 20;
}
