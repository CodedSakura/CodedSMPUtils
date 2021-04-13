package eu.codedsakura.fabricsmputils.modules.pvp;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;

public interface IPvPComponent extends ComponentV3 {
    boolean isOn();

    void set(boolean value);
}
