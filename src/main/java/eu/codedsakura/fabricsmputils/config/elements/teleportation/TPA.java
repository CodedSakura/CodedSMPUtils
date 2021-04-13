package eu.codedsakura.fabricsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.fabricsmputils.modules.tpa.TPACooldownMode;

public class TPA extends Teleportable {
    @Property public int timeout = 60;
    @Property("cooldown-mode") public TPACooldownMode cooldownMode = TPACooldownMode.WhoTeleported;
}
