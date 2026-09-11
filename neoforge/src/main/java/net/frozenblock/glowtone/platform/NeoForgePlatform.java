package net.frozenblock.glowtone.platform;

import com.mojang.datafixers.util.Pair;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.frozenblock.glowtone.GlowtoneConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class NeoForgePlatform implements CommonPlatform {

	@Override
	public boolean isFabric() {
		return false;
	}

	@Override
	public boolean isNeoForge() {
		return true;
	}

	@Override
	public boolean isModLoaded(String mod) {
		var modList = ModList.get();
		boolean loadingModCheck = FMLLoader.getCurrent().getLoadingModList().getModFileById(mod) != null;
		if (modList == null) return loadingModCheck;
		else return ModList.get().isLoaded(mod) || loadingModCheck;
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.getCurrent().isProduction();
	}

	@Override
	public Path getConfigDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	private static final Map<Identifier, PreparableReloadListener> RELOAD_LISTENERS = new LinkedHashMap<>();

	@Override
	public void registerResourceListener(String path, PreparableReloadListener listener) {
		RELOAD_LISTENERS.putIfAbsent(GlowtoneConstants.id(path), listener);
	}

	@SubscribeEvent
	public static void registerReloadListeners(AddClientReloadListenersEvent event) {
		RELOAD_LISTENERS.forEach(event::addListener);
	}

	public static Set<Pair<Identifier, GlowtonePackActivation>> PACK_LIST = new HashSet<>();

	@Override
	public void registerResourcePack(String path, GlowtonePackActivation activation) {
		PACK_LIST.add(new Pair<>(GlowtoneConstants.id(path), activation));
	}

	@SubscribeEvent
	public static void registerResourcePacks(AddPackFindersEvent event) {
		for (Pair<Identifier, GlowtonePackActivation> pair : PACK_LIST) {
			Identifier id = pair.getFirst();
			boolean required = pair.getSecond() == GlowtonePackActivation.ALWAYS_ENABLED;

			event.addPackFinders(
				Identifier.fromNamespaceAndPath(id.getNamespace(), "resourcepacks/" + id.getPath()),
				PackType.CLIENT_RESOURCES,
				Component.translatable("pack." + id.getNamespace() + "." + id.getPath()),
				PackSource.BUILT_IN,
				required,
				Pack.Position.TOP
			);
		}
	}

	@Override
	public void registerOnTickStart(Consumer<Minecraft> listener) {
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, _ -> listener.accept(Minecraft.getInstance()));
	}

	@Override
	public void registerOnTickEnd(Consumer<Minecraft> listener) {
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, _ -> listener.accept(Minecraft.getInstance()));
	}
}
