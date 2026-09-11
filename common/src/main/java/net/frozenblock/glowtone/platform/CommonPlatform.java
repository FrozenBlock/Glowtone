package net.frozenblock.glowtone.platform;

import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public interface CommonPlatform {

	boolean isFabric();

	boolean isNeoForge();

	boolean isModLoaded(String mod);

	boolean isDevelopmentEnvironment();

	Path getConfigDirectory();

	void registerResourceListener(String path, PreparableReloadListener listener);

	void registerResourcePack(String path, GlowtonePackActivation activation);

	void registerOnTickStart(Consumer<Minecraft> listener);

	void registerOnTickEnd(Consumer<Minecraft> listener);
}
