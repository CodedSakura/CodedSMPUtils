package eu.codedsakura.codedsmputils.modules.teleportation.warps;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

public class WarpListComponent implements IWarpListComponent {
    private final List<Warp> warps = new ArrayList<>();

    @Override
    public void readFromNbt(NbtCompound tag) {
        warps.clear();
        NbtList warpsTag = tag.getList("warps", NbtType.COMPOUND);
        for (NbtElement _warpTag : warpsTag) {
            NbtCompound warpTag = (NbtCompound) _warpTag;
            warps.add(new Warp(
                    warpTag.getDouble("x"),
                    warpTag.getDouble("y"),
                    warpTag.getDouble("z"),
                    warpTag.getFloat("yaw"),
                    warpTag.getFloat("pitch"),
                    warpTag.getString("name"),
                    warpTag.getUuid("owner"),
                    warpTag.getUuid("id")
            ));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList warpsTag = new NbtList();
        for (Warp warp : warps) {
            NbtCompound warpTag = new NbtCompound();
            warpTag.putUuid("id", warp.id);
            warpTag.putString("name", warp.name);
            warpTag.putUuid("owner", warp.owner);
            warpTag.putDouble("x", warp.x);
            warpTag.putDouble("y", warp.y);
            warpTag.putDouble("z", warp.z);
            warpTag.putFloat("yaw", warp.yaw);
            warpTag.putFloat("pitch", warp.pitch);
            warpsTag.add(warpTag);
        }
        tag.put("warps", warpsTag);
    }

    @Override
    public boolean addWarp(Warp warp) {
        if (warps.stream().anyMatch(warp1 -> warp1.name.equalsIgnoreCase(warp.name))) return false;
        return warps.add(warp);
    }

    @Override
    public boolean removeWarp(String name) {
        if (warps.stream().noneMatch(warp -> warp.name.equalsIgnoreCase(name))) return false;
        return warps.removeIf(warp -> warp.name.equalsIgnoreCase(name));
    }

    @Override
    public List<Warp> getWarps() {
        return warps;
    }
}
