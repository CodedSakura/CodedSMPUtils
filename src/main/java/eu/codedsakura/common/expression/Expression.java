package eu.codedsakura.common.expression;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

public abstract class Expression<T> {
    protected final String data;
    private final ScriptEngine engine;

    public Expression(String data) {
        this.data = data;
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("JavaScript");
    }

    private Map<String, String> processVariables(Map<String, ?> variables) {
        HashMap<String, String> out = new HashMap<>();
        variables.forEach((s, o) -> {
            String value = String.valueOf(o);
            // https://stackoverflow.com/a/55592455/8672525
            if (
                    value.matches("^[+-]?(\\d+([.]\\d*)?([eE][+-]?\\d+)?|[.]\\d+([eE][+-]?\\d+)?)\n$") ||
                    value.matches("^true|false$")) {
                out.put(s, value);
            } else {
                out.put(s, '"' + value + '"');
            }
        });
        return out;
    }

    protected Object getRawValue(Map<String, ?> variables) throws ScriptException {
        final String[] finalData = {data};
        processVariables(variables)
                .forEach((k, v) -> finalData[0] = finalData[0].replaceAll("\\$" + k, v));
        return engine.eval(finalData[0]);
    }

    abstract public T getValue(Map<String, ?> variables) throws ScriptException;
}
