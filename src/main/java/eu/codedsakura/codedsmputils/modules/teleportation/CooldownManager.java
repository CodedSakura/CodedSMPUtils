package eu.codedsakura.codedsmputils.modules.teleportation;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;

public class CooldownManager {
    private static final ArrayList<CooldownInstance<?>> cooldowns = new ArrayList<>();

    public static void addCooldown(Class<?> type, UUID player, int time) {
        cooldowns.add(new CooldownInstance<>(type, player, time));
        removePassed();
    }

    public static boolean hasCooldownEnded(Class<?> type, UUID player) {
        removePassed();
        return cooldowns.stream().noneMatch(c ->
                (CONFIG.teleportation.globalCooldown || c.type == type) && c.player == player && c.hasNotPassed());
    }

    public static int getCooldownTimeRemaining(Class<?> type, UUID player) {
        removePassed();
        List<CooldownInstance<?>> relevant = cooldowns.stream().filter(c ->
                (CONFIG.teleportation.globalCooldown || c.type == type) && c.player == player && c.hasNotPassed())
                .collect(Collectors.toList());
        if (relevant.size() == 0) return -1;
        return relevant.stream().map(CooldownInstance::timeRemaining).max(Comparator.naturalOrder()).get().intValue();
    }

    private static void removePassed() {
        final long epoch = Instant.now().getEpochSecond();
        cooldowns.removeIf(cooldownInstance -> cooldownInstance.hasPassed(epoch));
    }

    public static int clearAll() {
        cooldowns.clear();
        return 1;
    }

    private static class CooldownInstance<T> {
        private final Class<T> type;
        private final UUID player;
        private final int time;
        private final long start;

        public CooldownInstance(Class<T> type, UUID player, int time) {
            this.type = type;
            this.player = player;
            this.time = time;
            this.start = Instant.now().getEpochSecond();
        }

        long timePassed() { return Instant.now().getEpochSecond() - start; }
        long timeRemaining() { return time - timePassed(); }
        boolean hasNotPassed() { return timeRemaining() >= 0; }

        boolean hasPassed(long epoch) {
            return time - (epoch - start) < 0;
        }
    }
}
