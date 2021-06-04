package eu.codedsakura.codedsmputils.config.elements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class NoMobGrief {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    // whether enabled
    @Property public boolean wither = true;
    @Property public boolean ghast = true;
    @Property public boolean creeper = true;
    @Property public boolean enderman = true;
}
