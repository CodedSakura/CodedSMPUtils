package eu.codedsakura.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Notates that this class is a config root
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigFile {
    /**
     * Name of the config file, used for copying from resources, placing in config folder
     */
    String value();
}
