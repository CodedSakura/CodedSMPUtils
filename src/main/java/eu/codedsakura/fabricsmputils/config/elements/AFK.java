package eu.codedsakura.fabricsmputils.config.elements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class AFK {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property public int time = 300; // seconds
}
