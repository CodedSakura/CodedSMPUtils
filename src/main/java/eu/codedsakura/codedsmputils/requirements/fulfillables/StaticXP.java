package eu.codedsakura.codedsmputils.requirements.fulfillables;

import eu.codedsakura.common.ExperienceUtils;
import eu.codedsakura.common.Pair;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;

public class StaticXP extends Fulfillable {
    public boolean consume;
    public int value;
    public ExperienceUtils.Type type;
    private String data;

    public StaticXP(boolean consume, String data) {
        this.consume = consume;
        this.data = data;
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
                return null;
        }
    }

    @Override
    public String getChoiceLocale() {
        switch (type) {
            case POINTS:
                return "teleportation.back.requirements.choice.entry.xp.points";
            case LEVELS:
                return "teleportation.back.requirements.choice.entry.xp.levels";
            default:
                return null;
        }
    }

    @Override
    public String getOriginalValue() {
        return this.data;
    }

    public void removeFromPlayer(ServerPlayerEntity player) {
        switch (type) {
            case LEVELS:
                player.addExperienceLevels(-value);
                break;
            case POINTS:
                player.addExperience(-value);
                break;
        }
    }
}
