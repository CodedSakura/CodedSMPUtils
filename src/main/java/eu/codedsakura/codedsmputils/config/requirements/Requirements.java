package eu.codedsakura.codedsmputils.config.requirements;

import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Required;

import java.util.ArrayList;

public abstract class Requirements {
    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "Experience") public Experience experience = null;
    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "Items", list = true) public ArrayList<Items> items = new ArrayList<>();
    @Required(atLeastOneOfGroup = 1) @ChildNode(value = "Advancement", list = true) public ArrayList<Advancement> advancements = new ArrayList<>();
}
