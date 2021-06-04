package eu.codedsakura.codedsmputils.locales;

import eu.codedsakura.codedsmputils.CodedSMPUtils;
import eu.codedsakura.codedsmputils.config.CodedSMPUtilsConfig;
import eu.codedsakura.codedsmputils.config.elements.Locale;
import eu.codedsakura.common.expression.StringExpression;
import eu.pb4.placeholders.TextParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.logger;

public class LocaleManager {
    private final Properties props = new Properties();
    private final Properties overrides = new Properties();

    public void loadFromConfig(CodedSMPUtilsConfig config) throws IOException {
        props.clear();
        overrides.clear();

        InputStream inputStream;
        if (config.locale.matches("^[a-z]{2,3}$")) {
            inputStream = getClass().getClassLoader().getResourceAsStream(config.locale + ".locale");
        } else {
            File file = FabricLoader.getInstance().getConfigDir().resolve(config.locale).toFile();
            if (!file.exists()) {
                throw new FileNotFoundException(config.locale);
            }
            inputStream = new FileInputStream(file);
        }
        props.load(inputStream);

        for (Locale locale : config.locales)
            overrides.put("base." + locale.entry, locale.text);

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

    private static Compound findNextExpressions(String value) {
        int bracketCounter = 0;
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
                    return current;
                }
            } else if (bracketCounter > 0) {
                current.data += c;
            }
        }

        return null;
    }

    public Text get(String entry) {
        return get(entry, new HashMap<>());
    }

    public Text get(String entry, String fallback) {
        return get(entry, fallback, new HashMap<>());
    }

    public Text get(String entry, Map<String, ?> variables) {
        return get(entry, null, variables);
    }

    public Text get(String entry, String fallback, Map<String, ?> variables) {
        String text;
        try {
            text = getEntry(entry, fallback);
        } catch (LocaleError e) {
            e.printStackTrace();
            return new LiteralText("<Locale error, please report to an admin>");
        }

        text = parseVariables(text, variables);

        text = text.replaceAll("(?<!\\\\)\\[", "<").replaceAll("(?<!\\\\)]", ">");
        text = text.replaceAll("\\\\\\[", "[").replaceAll("\\\\]", "]");

//        return CodedSMPUtils.fsa.toNative(CodedSMPUtils.miniMessage.parse(text));
        return TextParser.parse(text);
    }

    public String getEntry(String entry, @Nullable String fallback) {
        String text = overrides.getProperty(entry);
        if (text == null)
            text = props.getProperty(entry);

        if (fallback == null && text == null) {
            logger.error("Locale '" + entry + "' not found!");
            throw new LocaleError("Locale '" + entry + "' not found!");
        } else if (text == null) {
            text = overrides.getProperty(fallback);
        }

        if (text == null) {
            text = props.getProperty(fallback);
        }
        if (text == null) {
            logger.error("Fallback Locale '" + entry + "' not found!");
            throw new LocaleError("Fallback Locale '" + entry + "' not found!", true);
        }

        return text;
    }

    public static String parseVariables(String text, Map<String, ?> variables) {
        StringBuilder sb = new StringBuilder(text);
        try {
            Compound compound;
            while ((compound = findNextExpressions(sb.toString())) != null) {
                sb.delete(compound.start, compound.end);
                sb.insert(compound.start, compound.getValue(variables));
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private static class Compound {
        int start, end;
        String data;

        public String getValue(Map<String, ?> variables) throws ScriptException {
            return new StringExpression(data).getValue(variables);
        }
    }

}
