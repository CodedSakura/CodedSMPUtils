package eu.codedsakura.codedsmputils.config.requirements;

import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.annotations.Child;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.IntExpression;

public class Items {
    @Required @Property public IntExpression count;
    @Required @Child public String name;
    @Property public BoolExpression consume = new BoolExpression("true");
}
