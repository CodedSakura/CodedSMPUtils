package eu.codedsakura.codedsmputils.config.requirements;

import eu.codedsakura.codedsmputils.locales.LocaleManager;
import eu.codedsakura.common.annotations.Child;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.common.expression.StringExpression;

import java.util.Map;

public class Experience {
    @Required @Child private String data;
    @Property public BoolExpression consume = new BoolExpression("true");

    public String getData(Map<String, ?> variables) {
        return LocaleManager.parseVariables(data, variables);
    }
}
