package eu.codedsakura.fabricsmputils;

import eu.codedsakura.common.ConfigParser;
import eu.codedsakura.common.TeleportUtils;
import eu.codedsakura.fabricsmputils.config.FabricSMPUtilsConfig;
import eu.codedsakura.fabricsmputils.locales.LocaleManager;
import eu.codedsakura.fabricsmputils.modules.homes.Homes;
import eu.codedsakura.fabricsmputils.modules.pvp.PVP;
import eu.codedsakura.fabricsmputils.modules.tpa.TPA;
import eu.codedsakura.fabricsmputils.modules.warps.Warps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.markdown.DiscordFlavor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricSMPUtils implements ModInitializer {
    public static final Logger logger = LogManager.getLogger("FabricSMPUtils");
    public static FabricSMPUtilsConfig CONFIG = null;
    public static MiniMessage miniMessage = null;
    public static LocaleManager L = new LocaleManager();
    public static FabricServerAudiences fsa = null;

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


        new TPA().initialize();
        new Warps().initialize();
        new Homes().initialize();
        new PVP().initialize();
        // TODO /spawn, /rtp, /bottle, /afk, noMobGrief
        // CONSIDER /trade, pet transfer to other player /disown?

    }
}
