package eu.codedsakura.fabricsmputils.config.elements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class Bottle {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("max-bottle") public int maxBottle = 0;
    @Property("max-bottle-levels") public int maxBottleLevels = 50;

    @Property("min-bottle") public int minBottle = 0;
    @Property("min-bottle-levels") public int minBottleLevels = 1;

    @Property public int cooldown = 0;
}
