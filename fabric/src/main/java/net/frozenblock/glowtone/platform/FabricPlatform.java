package net.frozenblock.glowtone.platform;

import java.nio.file.Path;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class FabricPlatform implements CommonPlatform {

	@Override
	public boolean isFabric() {
		return true;
	}

	@Override
	public boolean isNeoForge() {
		return false;
	}

	@Override
	public boolean isModLoaded(String mod) {
		return FabricLoader.getInstance().isModLoaded(mod);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	@Override
	public Path getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public void registerResourceListener(String path, PreparableReloadListener listener) {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(GlowtoneConstants.id(path), listener);
	}

	@Override
	public void registerResourcePack(String path, boolean required) {
		PackActivationType type;
		if (required) type = PackActivationType.ALWAYS_ENABLED;
		else type = PackActivationType.NORMAL;
		FabricLoader.getInstance().getModContainer(GlowtoneConstants.MOD_ID).ifPresent(container ->
			ResourceLoader.registerBuiltinPack(
				GlowtoneConstants.id(path),
				container,
				Component.translatable("pack." + GlowtoneConstants.MOD_ID + "." + path),
				type
			));
	}

	@Override
	public void registerOnTickStart(Consumer<Minecraft> listener) {
		ClientTickEvents.START_CLIENT_TICK.register(listener::accept);
	}

	@Override
	public void registerOnTickEnd(Consumer<Minecraft> listener) {
		ClientTickEvents.END_CLIENT_TICK.register(listener::accept);
	}
}
