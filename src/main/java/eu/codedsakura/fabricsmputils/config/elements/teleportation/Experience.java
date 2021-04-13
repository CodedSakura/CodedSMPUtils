package eu.codedsakura.fabricsmputils.config.elements.teleportation;

import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.IntExpression;

public class Experience {
    @Property public IntExpression levels = new IntExpression("0");
    @Property public IntExpression points = new IntExpression("0");
}
