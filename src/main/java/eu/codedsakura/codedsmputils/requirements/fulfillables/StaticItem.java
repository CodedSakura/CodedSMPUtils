package eu.codedsakura.codedsmputils.requirements.fulfillables;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;

public class StaticItem extends Fulfillable {
    public boolean consume;
    public int count;
    public Item item;

    public StaticItem(boolean consume, String name, int count) {
        this.consume = consume;
        this.count = count;
        this.item = Registry.ITEM.get(new Identifier(name));
    }

    public StaticItem(boolean consume, Item item, int count) {
        this.consume = consume;
        this.count = count;
        this.item = item;
    }

    @Override
    public HashMap<String, ?> asArguments() {
        return new HashMap<String, Object>() {{
            if (checked) put("fulfilled", fulfilled);
            put("consume", consume);
            put("count", count);
            put("locale_key", item.getTranslationKey());
        }};
    }

    @Override
    public String getMissingLocale() {
        return "teleportation.back.requirements.missing.entry.item";
    }
}

