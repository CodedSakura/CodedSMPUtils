package eu.codedsakura.fabricsmputils.modules.warps;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;

import java.util.List;

public interface IWarpListComponent extends ComponentV3 {
    List<Warp> getWarps();

    boolean addWarp(Warp warp);

    boolean removeWarp(String name);
}
