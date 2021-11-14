package eu.codedsakura.codedsmputils.modules.teleportation.warps;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WarpListComponent extends PersistentState {
    private static final String FILE_KEY = "csmpu_warps";
    private static final Set<ServerWorld> backwardsWorlds = new HashSet<>();

    public static WarpListComponent get(ServerWorld world) {
        WarpListComponent backwards = null;
        if (!backwardsWorlds.contains(world)) {
            backwardsWorlds.add(world);
            try {
                NbtCompound nbt = world.getPersistentStateManager().readNbt("cardinal_world_components",
                        SharedConstants.getGameVersion().getSaveVersion().getId());
                if (nbt != null) {
                    backwards = WarpListComponent.readBackwardsNbt(nbt.getCompound("data"));
                }
            } catch (IOException ignored) {
            }
        }
        WarpListComponent finalBackwards = backwards == null ? new WarpListComponent() :
                (backwards.warps.size() > 0 ? backwards : new WarpListComponent());
        return world.getPersistentStateManager().getOrCreate(WarpListComponent::readNbt, () -> finalBackwards, FILE_KEY);
    }

    public static WarpListComponent readNbt(NbtCompound nbt) {
        WarpListComponent warp = new WarpListComponent();
        warp.readFromNbt(nbt);
        return warp;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        writeToNbt(nbt);
        return nbt;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public static WarpListComponent readBackwardsNbt(NbtCompound nbt) {
        WarpListComponent warp = new WarpListComponent();
        NbtCompound tag = nbt.getCompound("cardinal_components");
        if (tag.contains("fabricwarps:warplist")) {
            warp.readFromNbt(tag.getCompound("fabricwarps:warplist"));
            return warp;
        }
        if (tag.contains("fabric-smp-utils:warps")) {
            warp.readFromNbt(tag.getCompound("fabric-smp-utils:warps"));
            return warp;
        }
        return null;
    }

    private final List<Warp> warps = new ArrayList<>();

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

    public boolean addWarp(Warp warp) {
        if (warps.stream().anyMatch(warp1 -> warp1.name.equalsIgnoreCase(warp.name))) return false;
        return warps.add(warp);
    }

    public boolean removeWarp(String name) {
        if (warps.stream().noneMatch(warp -> warp.name.equalsIgnoreCase(name))) return false;
        return warps.removeIf(warp -> warp.name.equalsIgnoreCase(name));
    }

    public List<Warp> getWarps() {
        return warps;
    }
}
