package eu.codedsakura.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Notates that the value is a child node, parsed separately
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ChildNode {
    /**
     * Node name
     */
    String value();

    /**
     * Whether there's multiple nodes expected, type must be a List
     */
    boolean list() default false;
}
