package net.frozenblock.glowtone.mixin.client.sodium.vertex;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.sodium.vertex.GTSodiumVertexFormat;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFeatures;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexFormats;
import net.frozenblock.glowtone.render.vertex.GlowtoneVertexLayout;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.frozenblock.glowtone.material.MaterialShaderPatcher;

@ClientOnly
@Mixin(CompactChunkVertex.class)
public class CompactChunkVertexMixin {
	@Shadow
	@Final
	public static int STRIDE;

	@Unique
	private static final int GLOWTONE$CONTACTS = 4;
	@Unique
	private static final int GLOWTONE$SKY_CHROMA_ABGR = ARGB.toABGR(ChromaBlender.NEUTRAL_ARGB);
	@Unique
	private static final int GLOWTONE$NO_EDGES_LE = Integer.reverseBytes(QuadEdges.NO_EDGES);
	@Unique
	private static final int[] GLOWTONE$NO_CONTACT_LE = glowtone$noContact();

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexFormat$Builder;build()Lcom/mojang/blaze3d/vertex/VertexFormat;",
			ordinal = 0
		)
	)
	private static VertexFormat glowtone$modifyBlockVertexFormat(VertexFormat.Builder instance, Operation<VertexFormat> original) {
		final GlowtoneVertexFeatures features = GlowtoneVertexFeatures.startup();
		final VertexFormat format = original.call(GTSodiumVertexFormat.appendTerrainAttributes(instance, features));
		final int vertexSize = format.getVertexSize();
		if (vertexSize < STRIDE) {
			throw new IllegalStateException(
				"Glowtone terrain vertex format is " + vertexSize + " bytes, which is smaller than Sodium's stride of " + STRIDE
			);
		}

		GTSodiumVertexFormat.setup(format);
		GlowtoneVertexFormats.reportStartup("sodium terrain", format, features);
		return format;
	}

	@Inject(method = "lambda$getEncoder$0", at = @At("HEAD"))
	private static void glowtone$preWrite(
		long ptr, int materialBits, ChunkVertexEncoder.Vertex[] vertices, int section, CallbackInfoReturnable<Long> info,
		@Share("glowtone$state") LocalRef<ChromaBaker.SectionState> stateRef,
		@Share("glowtone$layout") LocalRef<GlowtoneVertexLayout> layoutRef,
		@Share("glowtone$edges") LocalRef<QuadEdges> edgesRef,
		@Share("glowtone$fluid") LocalBooleanRef fluidRef,
		@Share("glowtone$flags") LocalIntRef flagsRef
	) {
		final ChromaBaker.SectionState state = ChromaBaker.state();
		stateRef.set(state);
		layoutRef.set(state.sodiumLayout());
		edgesRef.set(state.pendingEdges());
		fluidRef.set(state.fluidQuad());
		flagsRef.set((state.emissiveQuad() ? 0x000000FF : 0)
			| (BlockMaterialRenderer.quadShaderIndex() << 8));
	}

	@Inject(
		method = "lambda$getEncoder$0",
		at = @At(
			value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/api/memory/MemoryIntrinsics;putInt(JI)V",
			ordinal = 0
		)
	)
	private static void glowtone$writeGlowtoneAttributes(
		long ptr, int materialBits, ChunkVertexEncoder.Vertex[] vertices, int section, CallbackInfoReturnable<Long> info,
		@Local(name = "vertex") ChunkVertexEncoder.Vertex vertex,
		@Share("glowtone$state") LocalRef<ChromaBaker.SectionState> stateRef,
		@Share("glowtone$layout") LocalRef<GlowtoneVertexLayout> layoutRef,
		@Share("glowtone$edges") LocalRef<QuadEdges> edgesRef,
		@Share("glowtone$fluid") LocalBooleanRef fluidRef,
		@Share("glowtone$flags") LocalIntRef flagsRef
	) {
		final GlowtoneVertexLayout layout = layoutRef.get();
		final ChromaBaker.SectionState state = stateRef.get();

		if (layout.hasChroma()) {
			MemoryUtil.memPutInt(ptr + layout.chroma(), ARGB.toABGR(state.sample(vertex.x, vertex.y, vertex.z)));
			MemoryUtil.memPutInt(ptr + layout.skyChroma(), ARGB.toABGR(state.sampleSky(vertex.x, vertex.y, vertex.z)));

			if (MaterialShaderPatcher.anyQuadOffset()) {
				MemoryUtil.memPutByte(ptr + layout.chroma() + 3L, MaterialShaderPatcher.encodeQuadOffset(vertex.x - state.quadCentreX()));
				MemoryUtil.memPutByte(ptr + layout.skyChroma() + 3L, MaterialShaderPatcher.encodeQuadOffset(vertex.z - state.quadCentreZ()));
			}
		}

		if (layout.hasFlags()) MemoryUtil.memPutInt(ptr + layout.flags(), flagsRef.get());

		if (layout.hasEdges()) {
			final int edgeIndex = fluidRef.get() ? edgesRef.get().indexOf(vertex.x, vertex.y, vertex.z) : state.nextEdgeVertex();
			glowtone$writeEdges(ptr, layout, edgesRef.get(), edgeIndex);
		}
	}

	@ModifyExpressionValue(
		method = "lambda$getEncoder$0",
		at = @At(
			value = "CONSTANT",
			args = "longValue=20",
			ordinal = 0
		)
	)
	private static long glowtone$widenStrideAdvance(long original, @Share("glowtone$layout") LocalRef<GlowtoneVertexLayout> layoutRef) {
		final int vertexSize = layoutRef.get().vertexSize();
		if (original != STRIDE || vertexSize < STRIDE) {
			throw new IllegalStateException(
				"Glowtone patched a " + original + " byte stride advance, expected Sodium's " + STRIDE
					+ " widened to " + vertexSize
			);
		}

		return vertexSize;
	}

	@Unique
	private static int[] glowtone$noContact() {
		final int[] packed = new int[GLOWTONE$CONTACTS];
		for (int contact = 0; contact < GLOWTONE$CONTACTS; contact++) {
			packed[contact] = Integer.reverseBytes(GlowtoneContactRects.NONE[contact]);
		}
		return packed;
	}

	@Unique
	private static void glowtone$writeEdges(long pointer, GlowtoneVertexLayout layout, QuadEdges edges, int index) {
		if (index < 0) {
			MemoryUtil.memPutInt(pointer + layout.edge(), GLOWTONE$NO_EDGES_LE);
			MemoryUtil.memPutInt(pointer + layout.edgeMask(), 0);
			for (int contact = 0; contact < GLOWTONE$CONTACTS; contact++) {
				MemoryUtil.memPutInt(pointer + layout.contact(contact), GLOWTONE$NO_CONTACT_LE[contact]);
			}
			return;
		}

		MemoryUtil.memPutInt(pointer + layout.edge(), Integer.reverseBytes(edges.get(index)));
		MemoryUtil.memPutInt(pointer + layout.edgeMask(), Integer.reverseBytes(edges.mask(index)));
		for (int contact = 0; contact < GLOWTONE$CONTACTS; contact++) {
			MemoryUtil.memPutInt(pointer + layout.contact(contact), Integer.reverseBytes(edges.contact(contact)));
		}
	}
}
