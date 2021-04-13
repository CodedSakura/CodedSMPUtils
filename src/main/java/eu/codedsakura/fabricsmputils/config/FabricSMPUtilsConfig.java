package eu.codedsakura.fabricsmputils.config;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.ConfigFile;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.fabricsmputils.config.elements.AFK;
import eu.codedsakura.fabricsmputils.config.elements.Bottle;
import eu.codedsakura.fabricsmputils.config.elements.NoMobGrief;
import eu.codedsakura.fabricsmputils.config.elements.PVP;
import eu.codedsakura.fabricsmputils.config.elements.teleportation.Teleportation;

@ConfigFile("FabricSMPUtils.xml")
public class FabricSMPUtilsConfig {
    @Property("disable-cdtf") public boolean disableCrossDimTPFix = false;
    @Property public String locale = "en";

    @ChildNode("Teleportation") public Teleportation teleportation = null;
    @ChildNode("PVP") public PVP pvp = null;
    @ChildNode("Bottle") public Bottle bottle = null;
    @ChildNode("AFK") public AFK afk = null;
    @ChildNode("NoMobGrief") public NoMobGrief noMobGrief = null;
}
