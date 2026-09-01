package net.frozenblock.glowtone.mixin.client.colour.engine;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@ClientOnly
@Mixin(LayerLightSectionStorage.class)
public class LayerLightSectionStorageMixin {

	@WrapOperation(
		method = "setStoredLevel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/chunk/DataLayer;set(IIII)V"
		)
	)
	protected void glowtone$removeColorsOnSetTo0(DataLayer instance, int x, int y, int z, int val, Operation<Void> original) {
		if (val == 0) instance.glowtone$removeAllColors(x, y, z);
		original.call(instance, x, y, z, val);
	}
}
