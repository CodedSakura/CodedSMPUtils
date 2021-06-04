package eu.codedsakura.codedsmputils.modules.pvp;

import net.minecraft.nbt.NbtCompound;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;

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
    public void readFromNbt(NbtCompound tag) {
        isOn = tag.getBoolean("on");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putBoolean("on", isOn);
    }
}
