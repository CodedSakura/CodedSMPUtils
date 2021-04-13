package eu.codedsakura.fabricsmputils.config.elements.teleportation.homes;

import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.annotations.Child;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.IntExpression;

public class Items {
    @Required @Property public IntExpression count;
    @Required @Property public BoolExpression consume;
    @Required @Child public String name;
}
