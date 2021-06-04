package eu.codedsakura.codedsmputils.config.elements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class Bottle {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("max-bottle") public String maxBottle = "0";
    @Property("min-bottle") public String minBottle = "0";

    @Property public int cooldown = 0;
}
