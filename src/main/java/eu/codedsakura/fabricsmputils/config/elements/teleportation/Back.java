package eu.codedsakura.fabricsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;

import java.util.ArrayList;

public class Back extends Teleportable {
    @Property public int cooldown = 120;
    @Required @Property public BoolExpression cost;

    @Required(atLeastOneOfGroup = 1) @ChildNode("Experience") public Experience experience = null;
    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "Items", list = true) public ArrayList<Items> items = null;
}
