package eu.codedsakura.common.expression;

import javax.script.ScriptException;
import java.util.Map;

public class IntExpression extends Expression<Integer> {
    public IntExpression(String data) {
        super(data);
    }

    public Integer getValue(Map<String, ?> variables) throws ScriptException {
        return Integer.parseInt(super.getRawValue(variables).toString());
    }
}
