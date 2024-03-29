package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.codedsmputils.config.elements.Locale;
import eu.codedsakura.codedsmputils.config.elements.teleportation.homes.Homes;
import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;

import java.util.ArrayList;

public class Teleportation {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("boss-bar")
    public String bossBar = "purple"; // "disabled" if off
    @Property("action-bar")
    public boolean actionBar = false;
    @Property("global-cooldown")
    public boolean globalCooldown = false;
    @Property("allow-back")
    public boolean allowBack = true;

    @ChildNode("TPA")
    public TPA tpa = null;
    @ChildNode("RTP")
    public RTP rtp = null;
    @ChildNode("Spawn")
    public Spawn spawn = null;
    @ChildNode("Warps")
    public Warps warps = null;
    @ChildNode("Back")
    public Back back = null;
    @ChildNode("Homes")
    public Homes homes = null;
    @ChildNode("LastDeath")
    public LastDeath lastDeath = null;
}
