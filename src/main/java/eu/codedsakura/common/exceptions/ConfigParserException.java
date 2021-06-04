package eu.codedsakura.common.exceptions;

/**
 * @see java.lang.Exception
 */
public class ConfigParserException extends RuntimeException {
    public ConfigParserException(String text) {
        super(text);
    }
}
