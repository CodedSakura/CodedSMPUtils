package eu.codedsakura.common.expression;

import javax.script.ScriptException;
import java.util.Map;

public class BoolExpression extends Expression<Boolean> {
    public BoolExpression(String data) {
        super(data);
    }

    public Boolean getValue(Map<String, ?> variables) {
        return Boolean.parseBoolean(super.getRawValue(variables).toString());
    }
}
