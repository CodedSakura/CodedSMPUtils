package eu.codedsakura.codedsmputils.requirements.fulfillables;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.text.TranslatableText;

import java.util.HashMap;

public class FAdvancement extends Fulfillable {
    public Advancement advancement;

    public FAdvancement(Advancement advancement) {
        this.advancement = advancement;
    }

    @Override
    public String getMissingLocale() {
        return "teleportation.back.requirements.missing.entry.advancement";
    }

    @Override
    public String getChoiceLocale() {
        return null;
    }

    @Override
    public String getOriginalValue() {
        return null;
    }

    @Override
    public HashMap<String, ?> asArguments() {
        return new HashMap<String, Object>() {{
            if (checked) put("fulfilled", fulfilled);
            AdvancementDisplay display = advancement.getDisplay();
            assert display != null;
            put("locale_key_title", ((TranslatableText) display.getTitle()).getKey());
            put("locale_key_desc", ((TranslatableText) display.getDescription()).getKey());
        }};
    }
}
