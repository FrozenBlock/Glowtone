package net.frozenblock.glowtone;

import net.frozenblock.glowtone.platform.NeoForgePlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(GlowtoneConstants.MOD_ID)
public final class GlowtoneNeoForge {

	public GlowtoneNeoForge(IEventBus modBus) {
		GlowtoneClient.init();
		modBus.addListener(NeoForgePlatform::registerReloadListeners);
		modBus.addListener(NeoForgePlatform::registerResourcePacks);
	}
}
