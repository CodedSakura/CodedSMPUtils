package eu.codedsakura.codedsmputils.config.elements.teleportation;

import eu.codedsakura.codedsmputils.config.elements.Locale;
import eu.codedsakura.codedsmputils.requirements.Relation;
import eu.codedsakura.common.annotations.ChildNode;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.common.annotations.Required;
import eu.codedsakura.common.expression.BoolExpression;
import eu.codedsakura.codedsmputils.config.requirements.Requirements;

import java.util.ArrayList;

public class Back extends Requirements {
    @ChildNode(value = "Locale", list = true) public ArrayList<Locale> locales = new ArrayList<>();

    @Property("stand-still") public int standStill = 5;
    @Property public int cooldown = 120;

    @Property("boss-bar") public String bossBar = "purple"; // null if off
    @Property("action-bar") public boolean actionBar = false;
    @Property("allow-back") public boolean allowBack = false;

    @Required @Property public BoolExpression cost;
    @Property("requirement-relation") public Relation requirementRelation = Relation.OR;
}
