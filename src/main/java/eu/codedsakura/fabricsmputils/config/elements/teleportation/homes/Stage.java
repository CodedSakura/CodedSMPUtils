package eu.codedsakura.fabricsmputils.config.elements.teleportation.homes;

import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;

public class Stage extends HomeStage {
    @Required @Property("to-get") public int toGet;
}
