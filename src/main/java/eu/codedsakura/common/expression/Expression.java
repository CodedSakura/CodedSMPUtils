package eu.codedsakura.common.expression;

import eu.codedsakura.common.exceptions.ExpressionException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

public abstract class Expression<T> {
    private final String data;
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    public Expression(String data) {
        this.data = data;
    }

    private String processVariable(Object variable) {
        String value = String.valueOf(variable);
        // https://stackoverflow.com/a/55592455/8672525
        if (
                value.matches("^[+-]?(\\d+([.]\\d*)?([eE][+-]?\\d+)?|[.]\\d+([eE][+-]?\\d+)?)\n$") ||
                value.matches("^true|false$")) {
            return value;
        } else {
            return '"' + value + '"';
        }
    }

    protected Object getRawValue(Map<String, ?> variables) {
        final String[] finalData = {data};

        variables.entrySet().stream()
                .filter(entry -> data.contains("$" + entry.getKey())) // make sure we're processing only the necessary variables
                .forEach(entry -> finalData[0] = finalData[0].replaceAll("\\$" + entry.getKey() + "(?!\\w)", processVariable(entry.getValue())));
        try {
            return engine.eval(finalData[0]);
        } catch (ScriptException e) {
            e.printStackTrace();
            throw new ExpressionException(finalData[0]);
        }
    }

    abstract public T getValue(Map<String, ?> variables) throws ScriptException;
}
