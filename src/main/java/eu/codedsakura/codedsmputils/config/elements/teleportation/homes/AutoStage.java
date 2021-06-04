package eu.codedsakura.codedsmputils.config.elements.teleportation.homes;

import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;

public class AutoStage extends HomeStage {
    @Required @Property("starting-with") public int startingWith;
    @Property("ending-with") public int endingWith = Integer.MAX_VALUE;
}
