package eu.codedsakura.fabricsmputils.config.elements;

import eu.codedsakura.common.annotations.Child;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;

public class Locale {
    @Property @Required public String entry;
    @Child public String text;
}
