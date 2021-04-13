package eu.codedsakura.fabricsmputils.locales;

import eu.codedsakura.common.expression.Expression;
import eu.codedsakura.fabricsmputils.FabricSMPUtils;
import eu.codedsakura.fabricsmputils.config.FabricSMPUtilsConfig;
import eu.codedsakura.fabricsmputils.config.elements.Locale;
import net.minecraft.text.Text;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.logger;

public class LocaleManager {
    private final Properties props = new Properties();
    private final Properties overrides = new Properties();

    public LocaleManager(String resourceName) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        props.load(inputStream);
    }

    public void loadFromConfig(FabricSMPUtilsConfig config) {
        overrides.clear();

        if (config.teleportation != null) {
            for (Locale locale : config.teleportation.locales)
                overrides.put("teleportation." + locale.entry, locale.text);

            if (config.teleportation.tpa != null)
                for (Locale locale : config.teleportation.tpa.locales)
                    overrides.put("teleportation.tpa." + locale.entry, locale.text);

            if (config.teleportation.rtp != null)
                for (Locale locale : config.teleportation.rtp.locales)
                    overrides.put("teleportation.rtp." + locale.entry, locale.text);

            if (config.teleportation.spawn != null)
                for (Locale locale : config.teleportation.spawn.locales)
                    overrides.put("teleportation.spawn." + locale.entry, locale.text);

            if (config.teleportation.warps != null)
                for (Locale locale : config.teleportation.warps.locales)
                    overrides.put("teleportation.warps." + locale.entry, locale.text);

            if (config.teleportation.back != null)
                for (Locale locale : config.teleportation.back.locales)
                    overrides.put("teleportation.back." + locale.entry, locale.text);

            if (config.teleportation.homes != null)
                for (Locale locale : config.teleportation.homes.locales)
                    overrides.put("teleportation.homes." + locale.entry, locale.text);
        }

        if (config.pvp != null)
            for (Locale locale : config.pvp.locales)
                overrides.put("pvp." + locale.entry, locale.text);

        if (config.bottle != null)
            for (Locale locale : config.bottle.locales)
                overrides.put("bottle." + locale.entry, locale.text);

        if (config.afk != null)
            for (Locale locale : config.afk.locales)
                overrides.put("afk." + locale.entry, locale.text);
    }

    private Compound[] findAllExpressions(String value) {
        int bracketCounter = 0;
        ArrayList<Compound> out = new ArrayList<>();
        Compound current = null;

        char[] charArray = value.toCharArray();
        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            if (c == '{') {
                if (++bracketCounter == 1) {
                    current = new Compound();
                    current.start = i;
                    current.data = "";
                }
            } else if (c == '}') {
                assert current != null;
                if (--bracketCounter == 0) {
                    current.end = i+1;
                    out.add(current);
                }
            } else if (bracketCounter > 0) {
                assert current != null;
                current.data += c;
            }
        }

        return out.toArray(new Compound[] {});
    }

    public Text getText(String entry) {
        return getText(entry, new HashMap<>());
    }

    public Text getText(String entry, Map<String, ?> variables) {
        String text = overrides.getProperty(entry);
        if (text == null) text = props.getProperty(entry);
        if (text == null) {
            logger.error("Locale " + entry + " not found!");
            return null;
        }

        Compound[] compounds = findAllExpressions(text);

        StringBuilder sb = new StringBuilder(text);
        try {
            for (Compound compound : compounds) {
                sb.delete(compound.start, compound.end);
                sb.insert(compound.start, compound.getValue(variables));
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        text = sb.toString();

        text = text.replaceAll("(?<!\\\\)\\[", "<").replaceAll("(?<!\\\\)]", ">");
        text = text.replaceAll("\\\\\\[", "[").replaceAll("\\\\]", "]");

        return FabricSMPUtils.fsa.toNative(FabricSMPUtils.miniMessage.parse(text));
    }

    private static class Compound {
        int start, end;
        String data;

        public String getValue(Map<String, ?> variables) throws ScriptException {
            return new StringExpression(data).getValue(variables);
        }
    }

    private static class StringExpression extends Expression<String> {
        StringExpression(String data) {
            super(data);
        }

        @Override
        public String getValue(Map<String, ?> variables) throws ScriptException {
            return super.getRawValue(variables).toString();
        }
    }
}
