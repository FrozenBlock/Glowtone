package net.frozenblock.glowtone.mixin;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class GlowtoneMixinPlugin implements IMixinConfigPlugin {
	private boolean hasSodium;

	@Override
	public void onLoad(String mixinPackage) {
		this.hasSodium = GlowtoneMixinModList.isLoaded("sodium");
	}

	@Override
	@Nullable
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!GlowtoneMixinOptions.enabled(mixinClassName)) return false;
		if (mixinClassName.contains(".sodium.")) return this.hasSodium;
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	@Nullable
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
