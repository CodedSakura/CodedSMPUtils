package eu.codedsakura.codedsmputils.modules.pvp;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;

public class PvPComponent {
    public static final Map<UUID, PvPComponent> PVPS = new HashMap<>();
    private static final String NBT_KEY = "csmpu:pvp";

    public static void writeToNbt(NbtCompound nbt, UUID player) {
        PvPComponent pvp = PVPS.get(player);
        if (pvp == null) return;
        pvp.writeToNbt(nbt);
    }

    public static void readFromNbt(NbtCompound nbt, UUID player) {
        PvPComponent pvp = new PvPComponent();
        if (!nbt.contains(NBT_KEY)) /* backwards compatibility */ {
            if (nbt.contains("cardinal_components")) {
                NbtCompound tag = nbt.getCompound("cardinal_components");
                if (tag.contains("fabricpvp:pvp")) {
                    pvp.readFromNbt(nbt.getCompound("fabrichomes:homes"));
                }
                if (tag.contains("fabric-smp-utils:pvp")) {
                    pvp.readFromNbt(nbt.getCompound("fabric-smp-utils:homes"));
                }
            }
        } else {
            pvp.readFromNbt(nbt.getCompound(NBT_KEY));
        }
        pvp.readFromNbt(nbt);
        PVPS.put(player, pvp);
    }

    public static PvPComponent get(UUID player) {
        if (!PVPS.containsKey(player)) PVPS.put(player, new PvPComponent());
        return PVPS.get(player);
    }

    public static PvPComponent get(PlayerEntity player) {
        return PvPComponent.get(player.getUuid());
    }

    private boolean isOn = CONFIG.pvp != null && CONFIG.pvp.defaultState;

    public boolean isOn() {
        return isOn;
    }

    public void set(boolean value) {
        isOn = value;
    }

    public void readFromNbt(NbtCompound tag) {
        isOn = tag.getBoolean("on");
    }

    public void writeToNbt(NbtCompound tag) {
        tag.putBoolean("on", isOn);
    }
}
