package eu.codedsakura.codedsmputils.config;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.ConfigFile;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.codedsmputils.config.elements.*;
import eu.codedsakura.codedsmputils.config.elements.teleportation.Teleportation;

import java.util.ArrayList;

@ConfigFile("CodedSMPUtils.xml")
public class CodedSMPUtilsConfig {
    @Property("disable-cdtf") public boolean disableCrossDimTPFix = false;
    @Property public String locale = "en";

    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @ChildNode("Teleportation") public Teleportation teleportation = null;
    @ChildNode("PVP") public PVP pvp = null;
    @ChildNode("Bottle") public Bottle bottle = null;
    @ChildNode("AFK") public AFK afk = null;
    @ChildNode("NoMobGrief") public NoMobGrief noMobGrief = null;
}
