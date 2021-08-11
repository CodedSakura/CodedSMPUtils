package eu.codedsakura.codedsmputils.modules.teleportation.homes;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.*;

public class HomeDataComponent {
    private static final Map<UUID, HomeDataComponent> HOMES = new HashMap<>();
    private static final String NBT_KEY = "csmpu:homes";

    public static void writeToNbt(NbtCompound nbt, UUID player) {
        HomeDataComponent home = HOMES.get(player);
        if (home == null) return;
        NbtCompound tag = new NbtCompound();
        home.writeToNbt(tag);
        nbt.put(NBT_KEY, tag);
    }

    public static void readFromNbt(NbtCompound nbt, UUID player) {
        HomeDataComponent home = new HomeDataComponent();
        if (!nbt.contains(NBT_KEY)) /* backwards compatibility */ {
            if (nbt.contains("cardinal_components")) {
                NbtCompound tag = nbt.getCompound("cardinal_components");
                if (tag.contains("fabrichomes:homes")) {
                    home.readFromNbt(nbt.getCompound("fabrichomes:homes"));
                }
                if (tag.contains("fabric-smp-utils:homes")) {
                    home.readFromNbt(nbt.getCompound("fabric-smp-utils:homes"));
                }
            }
        } else {
            home.readFromNbt(nbt.getCompound(NBT_KEY));
        }
        HOMES.put(player, home);
    }

    public static HomeDataComponent get(UUID player) {
        if (!HOMES.containsKey(player)) HOMES.put(player, new HomeDataComponent());
        return HOMES.get(player);
    }

    public static HomeDataComponent get(PlayerEntity player) {
        return HomeDataComponent.get(player.getUuid());
    }

    private final List<HomeComponent> homes = new ArrayList<>();
    private int maxHomes;

    public void readFromNbt(NbtCompound tag) {
        homes.clear();
        tag.getList("homes", NbtType.COMPOUND).forEach(v -> homes.add(HomeComponent.readFromNbt((NbtCompound) v)));
        maxHomes = tag.getInt("maxHomes");
    }

    public void writeToNbt(NbtCompound tag) {
        NbtList homeTag = new NbtList();
        homes.forEach(v -> {
            NbtCompound ct = new NbtCompound();
            v.writeToNbt(ct);
            homeTag.add(ct);
        });
        tag.put("homes", homeTag);
        tag.putInt("maxHomes", maxHomes);
    }

    public List<HomeComponent> getHomes() {
        return homes;
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    public boolean addHome(HomeComponent home) {
        if (homes.stream().anyMatch(v -> v.getName().equalsIgnoreCase(home.getName()))) return false;
        return homes.add(home);
    }

    public boolean removeHome(String name) {
        if (homes.stream().noneMatch(v -> v.getName().equalsIgnoreCase(name))) return false;
        return homes.removeIf(v -> v.getName().equalsIgnoreCase(name));
    }
}
