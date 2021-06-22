package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.codedsmputils.modules.teleportation.rtp.RTPAreaCenter;
import eu.codedsakura.codedsmputils.modules.teleportation.rtp.RTPAreaShape;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;

public class RTP extends Teleportable {
    @Property public int cooldown = 120;

    @Required @Property("max-range") public int maxRange;
    @Property("min-range") public int minRange = 0;

    @Property("area-shape") public RTPAreaShape areaShape = RTPAreaShape.Circle;
    @Property("area-center") public RTPAreaCenter areaCenter = RTPAreaCenter.Zero;
}
