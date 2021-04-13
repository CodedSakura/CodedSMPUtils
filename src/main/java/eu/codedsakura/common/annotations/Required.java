package eu.codedsakura.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Notates that this field is required
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {
    /**
     * Notates a group, in which at least one of multiple different fields is required
     */
    int atLeastOneOfGroup() default -1;
}
