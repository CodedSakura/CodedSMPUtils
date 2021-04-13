package eu.codedsakura.fabricsmputils.config.elements.teleportation.homes;

import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.IntExpression;
import eu.codedsakura.common.annotations.Property;

public class Experience {
    @Property public IntExpression levels = new IntExpression("0");
    @Property public IntExpression points = new IntExpression("0");
    @Required @Property public BoolExpression consume;
}
