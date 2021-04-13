package eu.codedsakura.common;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TeleportUtils {
    private static final ArrayList<CounterRunnable> tasks = new ArrayList<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tasks.forEach(CounterRunnable::run);
            tasks.removeIf(CounterRunnable::shouldRemove);
        });
    }

    public static void genericTeleport(String bossBar, Boolean actionBar, double standStillTime, ServerPlayerEntity who, Runnable onCounterDone) {
        MinecraftServer server = who.server;
        final Vec3d[] lastPos = {who.getPos()};

        bossBar = bossBar.toUpperCase();
        boolean useBossBar = bossBar.equals("DISABLED");

        CommandBossBar standStillBar = null;
        if (useBossBar) {
            standStillBar = server.getBossBarManager().add(new Identifier("standstill"), LiteralText.EMPTY);
            standStillBar.addPlayer(who);
            standStillBar.setColor(BossBar.Color.byName(bossBar));
        }
        who.networkHandler.sendPacket(new TitleS2CPacket(0, 10, 5));
        CommandBossBar finalStandStillBar = standStillBar;
        int standStillInTicks = (int) (standStillTime * 20);

        tasks.add(new CounterRunnable(standStillInTicks) {
            @Override
            void run() {
                if (counter == 0) {
                    if (useBossBar) {
                        finalStandStillBar.removePlayer(who);
                        server.getBossBarManager().remove(finalStandStillBar);
                    }
                    if (actionBar) {
                        who.sendMessage(new LiteralText("Teleporting!").formatted(Formatting.LIGHT_PURPLE), true);
                    }
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            who.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.RESET, null));
                        }
                    }, 500);
                    onCounterDone.run();
                    counter = -1;
                    return;
                }

                Vec3d currPos = who.getPos();
                double dist = lastPos[0].distanceTo(currPos);
                if (dist < .05) {
                    if (dist != 0) lastPos[0] = currPos;
                    counter--;
                } else {
                    lastPos[0] = currPos;
                    counter = standStillInTicks;
                }

                if (useBossBar) {
                    finalStandStillBar.setPercent((float) counter / standStillInTicks);
                }
                if (actionBar) {
                    who.sendMessage(new LiteralText("Stand still for ").formatted(Formatting.LIGHT_PURPLE)
                            .append(new LiteralText(Integer.toString((int) Math.floor((counter / 20f) + 1))).formatted(Formatting.GOLD))
                            .append(new LiteralText(" more seconds!").formatted(Formatting.LIGHT_PURPLE)), true);
                }

                who.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.SUBTITLE,
                        new LiteralText("Please stand still...").formatted(Formatting.RED, Formatting.ITALIC)));
                who.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE,
                        new LiteralText("Teleporting!").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)));
            }
        });
    }

    private abstract static class CounterRunnable {
        int counter;

        CounterRunnable(int counter) {
            this.counter = counter;
        }

        void run() {
            counter--;
        }

        boolean shouldRemove() {
            return counter < 0;
        }
    }
}
