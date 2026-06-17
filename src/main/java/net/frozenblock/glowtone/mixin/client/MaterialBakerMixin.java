package net.frozenblock.glowtone.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MaterialBaker.class)
public class MaterialBakerMixin {
	@WrapWithCondition(
		method = "lambda$logMissingTextures$0",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
		)
	)
	private static boolean glowtone$ignoreEmissiveLoggingA(Logger instance, String string, Object object1, Object object2) {
		if (object2 instanceof String object2String) return !object2String.endsWith("_glowtone_emissive");
		return true;
	}

	@WrapWithCondition(
		method = "lambda$logMissingTextures$2",
		at = @At(
			value = "INVOKE",
			target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
		)
	)
	private static boolean glowtone$ignoreEmissiveLoggingB(Logger instance, String string, Object object1, Object object2) {
		if (object2 instanceof String object2String) return !object2String.endsWith("_glowtone_emissive");
		return true;
	}
}
