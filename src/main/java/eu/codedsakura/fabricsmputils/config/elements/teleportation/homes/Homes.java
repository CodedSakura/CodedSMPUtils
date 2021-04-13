package eu.codedsakura.fabricsmputils.config.elements.teleportation.homes;

import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.fabricsmputils.config.elements.teleportation.Teleportable;

import java.util.ArrayList;

public class Homes extends Teleportable {
    @Property("stand-still") public int standStill = 3;

    @Property("allow-back") public BoolExpression allowBack = new BoolExpression("true");

    @Required @Property public int starting;
    @Required @Property public int max;

    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "Stage", list = true) public ArrayList<Stage> stages = new ArrayList<>();
    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "AutoStage", list = true) public ArrayList<AutoStage> autoStages = new ArrayList<>();
}
