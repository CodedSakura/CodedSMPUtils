package eu.codedsakura.fabricsmputils.modules.pvp;

import net.minecraft.nbt.CompoundTag;

import static eu.codedsakura.fabricsmputils.FabricSMPUtils.config;

public class PvPComponent implements IPvPComponent {
    private boolean isOn = config.pvp != null && config.pvp.defaultState;

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
