package eu.codedsakura.common.expression;

import eu.codedsakura.common.expression.Expression;

import javax.script.ScriptException;
import java.util.Map;

public class StringExpression extends Expression<String> {
    public StringExpression(String data) {
        super(data);
    }

    @Override
    public String getValue(Map<String, ?> variables) {
        return super.getRawValue(variables).toString();
    }
}
