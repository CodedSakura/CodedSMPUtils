package eu.codedsakura.fabricsmputils;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer;
import eu.codedsakura.fabricsmputils.modules.teleportation.homes.HomeDataComponent;
import eu.codedsakura.fabricsmputils.modules.teleportation.homes.IHomeDataComponent;
import eu.codedsakura.fabricsmputils.modules.teleportation.pvp.IPvPComponent;
import eu.codedsakura.fabricsmputils.modules.teleportation.pvp.PvPComponent;
import eu.codedsakura.fabricsmputils.modules.teleportation.warps.IWarpListComponent;
import eu.codedsakura.fabricsmputils.modules.teleportation.warps.WarpListComponent;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.minecraft.util.Identifier;

public class SMPUtilCardinalComponents implements WorldComponentInitializer, EntityComponentInitializer {
    public static final ComponentKey<IWarpListComponent> WARP_LIST =
            ComponentRegistryV3.INSTANCE.getOrCreate(new Identifier("fabric-smp-utils", "warps"), IWarpListComponent.class);
    public static final ComponentKey<IHomeDataComponent> HOME_DATA =
            ComponentRegistryV3.INSTANCE.getOrCreate(new Identifier("fabric-smp-utils", "homes"), IHomeDataComponent.class);
    public static final ComponentKey<IPvPComponent> PVP_DATA =
            ComponentRegistryV3.INSTANCE.getOrCreate(new Identifier("fabric-smp-utils", "pvp"), IPvPComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(HOME_DATA, playerEntity -> new HomeDataComponent(), RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(PVP_DATA, playerEntity -> new PvPComponent(), RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(WARP_LIST, WarpListComponent.class, world -> new WarpListComponent());
    }
}
