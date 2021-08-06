package eu.codedsakura.codedsmputils;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.codedsakura.codedsmputils.config.CodedSMPUtilsConfig;
import eu.codedsakura.codedsmputils.config.elements.teleportation.homes.AutoStage;
import eu.codedsakura.codedsmputils.config.elements.teleportation.homes.Stage;
import eu.codedsakura.codedsmputils.locales.LocaleManager;
import eu.codedsakura.codedsmputils.modules.pvp.PVP;
import eu.codedsakura.codedsmputils.modules.teleportation.CooldownManager;
import eu.codedsakura.codedsmputils.modules.teleportation.back.Back;
import eu.codedsakura.codedsmputils.modules.teleportation.homes.Homes;
import eu.codedsakura.codedsmputils.modules.teleportation.lastdeath.LastDeath;
import eu.codedsakura.codedsmputils.modules.teleportation.rtp.RTP;
import eu.codedsakura.codedsmputils.modules.teleportation.tpa.TPA;
import eu.codedsakura.codedsmputils.modules.teleportation.warps.Warps;
import eu.codedsakura.codedsmputils.requirements.RequirementManager;
import eu.codedsakura.common.ConfigParser;
import eu.codedsakura.common.TeleportUtils;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CodedSMPUtils implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("CodedSMPUtils");
    public static CodedSMPUtilsConfig CONFIG = null;
    public static LocaleManager L = new LocaleManager();

    public static Event<ConfigReloadEvent<CodedSMPUtilsConfig>> CONFIG_RELOAD_EVENT =
            EventFactory.createArrayBacked(ConfigReloadEvent.class, callbacks -> (oldConfig, newConfig) -> {
        for (ConfigReloadEvent<CodedSMPUtilsConfig> callback : callbacks) {
			callback.onConfigReload(oldConfig, newConfig);
		}
    });

    public interface ConfigReloadEvent<T> {
        void onConfigReload(T oldConfig, T newConfig);
    }

    @Override
    public void onInitialize() {
        logger.info("CSMPU initializing...");

        if (CONFIG == null) loadConfig();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                if (CONFIG.teleportation != null) {
                    if (CONFIG.teleportation.back != null)
                        RequirementManager.verifyRequirements(CONFIG.teleportation.back, server);

                    if (CONFIG.teleportation.homes != null) {
                        for (AutoStage autoStage : CONFIG.teleportation.homes.autoStages)
                            RequirementManager.verifyRequirements(autoStage, server);
                        for (Stage stage : CONFIG.teleportation.homes.stages)
                            RequirementManager.verifyRequirements(stage, server);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        TeleportUtils.initialize();

//        miniMessage = MiniMessage.builder()
//                .markdownFlavor(DiscordFlavor.get())
//                .build();


        // TODO /spawn, /bottle, /afk
        // CONSIDER /trade, pet transfer to other player /disown?

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("csmpu")
                    .requires(Permissions.require("codedspmutils.main-command", 2))
                    .then(literal("reload-config")
                            .requires(Permissions.require("codedspmutils.main-command.config-reload", 3))
                            .executes(this::reloadConfig))
                    .then(literal("clear-cooldowns")
                            .requires(Permissions.require("codedspmutils.main-command.clear-cooldowns", 2))
                            .executes(ctx -> CooldownManager.clearAll()))
                    .then(literal("debug")
                            .requires(Permissions.require("codedspmutils.main-command.debug", 4))
                            .then(literal("locale")
                                    .then(argument("name", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                ctx.getSource().sendFeedback(new LiteralText(
                                                        L.getEntry(StringArgumentType.getString(ctx, "name"), null)), false);
                                                return 1;
                                            })))
                            .then(literal("text-parser")
                                    .then(argument("data", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                ctx.getSource().sendFeedback(TextParser.parse(StringArgumentType.getString(ctx, "data")), false);
                                                return 1;
                                            })))));


            new TPA(dispatcher);
            new Warps(dispatcher);
            new Homes(dispatcher);
            new PVP(dispatcher);
            new Back(dispatcher);
            new RTP(dispatcher);
            new LastDeath(dispatcher);
        });
    }

    public static void loadConfig() {
        logger.info("CSMPU loading config...");
        try {
            CONFIG = new ConfigParser<>(CodedSMPUtilsConfig.class, FabricLoader.getInstance().getConfigDir()).read();
            L.loadFromConfig(CONFIG);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        CodedSMPUtilsConfig oldConfig = CONFIG;
        try {
            CONFIG = new ConfigParser<>(CodedSMPUtilsConfig.class, FabricLoader.getInstance().getConfigDir()).read();
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
