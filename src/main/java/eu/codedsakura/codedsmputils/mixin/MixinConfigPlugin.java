package eu.codedsakura.codedsmputils.mixin;

import eu.codedsakura.codedsmputils.CodedSMPUtils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

import static eu.codedsakura.codedsmputils.CodedSMPUtils.CONFIG;
import static eu.codedsakura.codedsmputils.CodedSMPUtils.logger;

public class MixinConfigPlugin implements IMixinConfigPlugin {
    private final int packageNameLength = this.getClass().getPackage().getName().length();

    @Override
    public void onLoad(String mixinPackage) {
        logger.info("CSMPU mixins initializing...");
        CodedSMPUtils.loadConfig();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String[] mixin = mixinClassName.substring(packageNameLength).split("\\.");
        if (mixin.length == 1) {
            String name = mixin[0];
            switch (name) {
                case "PVPMixin":
                    return CONFIG.pvp != null;
                case "TPMixin":
                    return CONFIG.disableCrossDimTPFix;
            }
        } else if (mixin.length == 2) {
            String packageName = mixin[0];
            String mixinName = mixin[1];
            if (packageName.equals("nomobgrief")) {
                if (CONFIG.noMobGrief == null) return false;
                switch (mixinName) {
                    case "CreeperExplosionMixin":
                        return CONFIG.noMobGrief.creeper;
                    case "EndermanBlockPickupMixin":
                        return CONFIG.noMobGrief.enderman;
                    case "FireballExplosionMixin":
                        return CONFIG.noMobGrief.ghast;
                    case "WitherSkullExplosionMixin":
                        return CONFIG.noMobGrief.wither;
                }
            }
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
