package eu.codedsakura.fabricsmputils;

import com.mojang.brigadier.context.CommandContext;
import eu.codedsakura.common.ConfigParser;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.fabricsmputils.config.FabricSMPUtilsConfig;
import eu.codedsakura.fabricsmputils.locales.LocaleManager;
import eu.codedsakura.fabricsmputils.modules.teleportation.homes.Homes;
import eu.codedsakura.fabricsmputils.modules.teleportation.pvp.PVP;
import eu.codedsakura.fabricsmputils.modules.teleportation.tpa.TPA;
import eu.codedsakura.fabricsmputils.modules.teleportation.warps.Warps;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.markdown.DiscordFlavor;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.server.command.CommandManager.literal;

public class FabricSMPUtils implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("FabricSMPUtils");
    public static FabricSMPUtilsConfig CONFIG = null;
    public static MiniMessage miniMessage = null;
    public static LocaleManager L = new LocaleManager();
    public static FabricServerAudiences fsa = null;

    public static Event<ConfigReloadEvent<FabricSMPUtilsConfig>> CONFIG_RELOAD_EVENT =
            EventFactory.createArrayBacked(ConfigReloadEvent.class, callbacks -> (oldConfig, newConfig) -> {
        for (ConfigReloadEvent<FabricSMPUtilsConfig> callback : callbacks) {
			callback.onConfigReload(oldConfig, newConfig);
		}
    });

    public interface ConfigReloadEvent<T> {
        void onConfigReload(T oldConfig, T newConfig);
    }

    @Override
    public void onInitialize() {
        logger.info("FSMPU initializing...");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> fsa = FabricServerAudiences.of(server));

        TeleportUtils.initialize();
        try {
            CONFIG = new ConfigParser<>(FabricSMPUtilsConfig.class, FabricLoader.getInstance().getConfigDir()).read();
            L.loadFromConfig(CONFIG);
        } catch (Exception e) {
            e.printStackTrace();
        }

        miniMessage = MiniMessage.builder()
                .markdownFlavor(DiscordFlavor.get())
                .build();


        // TODO /spawn, /rtp, /bottle, /afk, noMobGrief
        // CONSIDER /trade, pet transfer to other player /disown?

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("fsmpu")
                    .requires(Permissions.require("fabricspmutils.config-reload", 3))
                    .then(literal("reload-config").executes(this::reloadConfig)));

            new TPA(dispatcher);
            new Warps(dispatcher);
            new Homes(dispatcher);
            new PVP(dispatcher);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {});
    }

    private int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        FabricSMPUtilsConfig oldConfig = CONFIG;
        try {
            CONFIG = new ConfigParser<>(FabricSMPUtilsConfig.class, FabricLoader.getInstance().getConfigDir()).read();
            L.loadFromConfig(CONFIG);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.getSource().sendFeedback(L.get("base.reload.failed"), true);
            return 1;
        }

        CONFIG_RELOAD_EVENT.invoker().onConfigReload(oldConfig, CONFIG);

        if (oldConfig.disableCrossDimTPFix != CONFIG.disableCrossDimTPFix ||
                (oldConfig.pvp == null) != (CONFIG.pvp == null) ||
                (oldConfig.noMobGrief == null) != (CONFIG.noMobGrief == null)) {
            ctx.getSource().sendFeedback(L.get("base.reload.partial"), true);
        } else {
            ctx.getSource().sendFeedback(L.get("base.reload.success"), true);
        }
        return 0;
    }
}
