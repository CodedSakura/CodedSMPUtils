package eu.codedsakura.fabricsmputils.modules.teleportation.pvp;

import net.minecraft.nbt.CompoundTag;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.CONFIG;

public class PvPComponent implements IPvPComponent {
    private boolean isOn = CONFIG.pvp != null && CONFIG.pvp.defaultState;

    @Override
    public boolean isOn() {
        return isOn;
    }

    @Override
    public void set(boolean value) {
        isOn = value;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        isOn = tag.getBoolean("on");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putBoolean("on", isOn);
    }
}
