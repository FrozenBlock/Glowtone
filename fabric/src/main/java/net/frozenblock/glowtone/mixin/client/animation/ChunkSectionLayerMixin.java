package net.frozenblock.glowtone.mixin.client.animation;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.frozenblock.glowtone.animation.BlockAnimationType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {

	@SuppressWarnings("InvokerTarget")
	@Invoker("<init>")
	private static ChunkSectionLayer glowtone$newChunkSectionLayer(
		String internalName,
		int ordinal,
		RenderPipeline pipeline,
		int bufferSize,
		boolean translucent
	) {
		throw new AssertionError("Mixin injection failed - Glowtone ChunkSectionLayerMixin");
	}

	@SuppressWarnings("ShadowTarget")
	@Shadow
	@Final
	@Mutable
	private static ChunkSectionLayer[] $VALUES;

	@Inject(
		method = "<clinit>",
		at = @At(
			value = "FIELD",
			opcode = Opcodes.PUTSTATIC,
			target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;$VALUES:[Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;",
			shift = At.Shift.AFTER
		)
	)
	private static void glowtone$addBlockAnimationSectionLayers(CallbackInfo info) {
		final var chunkSectionLayers = new ArrayList<>(Arrays.asList($VALUES));
		final var last = chunkSectionLayers.getLast();
		final AtomicInteger currentOrdinal = new AtomicInteger(last.ordinal());

		Arrays.stream(BlockAnimationType.values()).forEach(type -> {
			final String baseInternalName = "GLOWTONE_" + type.name();

			final ChunkSectionLayer newSolidLayer = glowtone$newChunkSectionLayer(
				baseInternalName + "_SOLID",
				currentOrdinal.incrementAndGet(),
				type.solidPipeline(),
				4194304,
				false
			);
			chunkSectionLayers.add(newSolidLayer);

			final ChunkSectionLayer newCutoutLayer = glowtone$newChunkSectionLayer(
				baseInternalName + "_CUTOUT",
				currentOrdinal.incrementAndGet(),
				type.cutoutPipeline(),
				4194304,
				false
			);
			chunkSectionLayers.add(newCutoutLayer);

			final ChunkSectionLayer newTranslucentLayer = glowtone$newChunkSectionLayer(
				baseInternalName + "_TRANSLUCENT",
				currentOrdinal.incrementAndGet(),
				type.translucentPipeline(),
				786432,
				true
			);
			chunkSectionLayers.add(newTranslucentLayer);
		});

		$VALUES = chunkSectionLayers.toArray(new ChunkSectionLayer[0]);
	}
}
