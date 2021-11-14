package eu.codedsakura.common;

import net.minecraft.entity.player.PlayerEntity;

public class ExperienceUtils {
    public enum Type { LEVELS, POINTS }

    public static Pair<Type, Integer> parseXPString(String text) {
        Type t = Type.POINTS;
        if (text.toLowerCase().endsWith("l")) {
            t = Type.LEVELS;
            text = text.substring(0, text.length() - 1);
        }
        return new Pair<>(t, Integer.parseInt(text));
    }

    public static long levelToTotalPoints(long n) {
        if (n < 15) {
            return n * n + n * 6;
        } else if (n < 30) {
            return ((5 * n * n - 81 * n) >> 1) + 360;
        } else {
            return ((9 * n * n - 325 * n) >> 1) + 2220;
        }
    }

    public static long playerXPToPoints(PlayerEntity player) {
        return levelToTotalPoints(player.experienceLevel) + (int)(player.experienceProgress * player.getNextLevelExperience());
    }
}
