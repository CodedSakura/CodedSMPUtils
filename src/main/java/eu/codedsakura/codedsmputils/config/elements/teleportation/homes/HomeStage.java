package eu.codedsakura.codedsmputils.config.elements.teleportation.homes;

import eu.codedsakura.codedsmputils.requirements.Relation;
import eu.codedsakura.common.annotations.Property;
import eu.codedsakura.codedsmputils.config.requirements.Requirements;

public abstract class HomeStage extends Requirements {
    @Property("requirement-relation") public Relation requirementRelation = Relation.AND;
}
