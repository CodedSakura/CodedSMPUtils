package eu.codedsakura.codedsmputils.requirements.fulfillables;

import eu.codedsakura.common.ExperienceUtils;
import eu.codedsakura.common.Pair;

import java.util.HashMap;

public class StaticXP extends Fulfillable {
    public boolean consume;
    public int value;
    public ExperienceUtils.Type type;

    public StaticXP(boolean consume, String data) {
        this.consume = consume;
        Pair<ExperienceUtils.Type, Integer> pair = ExperienceUtils.parseXPString(data);
        type = pair.getLeft();
        value = pair.getRight();
    }

    @Override
    public HashMap<String, ?> asArguments() {
        return new HashMap<String, Object>() {{
            if (checked) put("fulfilled", fulfilled);
            put("consume", consume);
            put("count", value);
        }};
    }

    @Override
    public String getMissingLocale() {
        switch (type) {
            case POINTS:
                return "teleportation.back.requirements.missing.entry.xp.points";
            case LEVELS:
                return "teleportation.back.requirements.missing.entry.xp.levels";
            default:
                return "";
        }
    }
}
