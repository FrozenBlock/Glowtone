package net.frozenblock.glowtone.mixin.client.colour.sodium.vertex;

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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.frozenblock.glowtone.light.color.render.ChromaBaker;
import net.frozenblock.glowtone.light.color.render.ChromaBlender;
import net.frozenblock.glowtone.light.edge.QuadEdges;
import net.frozenblock.glowtone.render.GlowtoneContactRects;
import net.frozenblock.glowtone.render.sodium.vertex.GTSodiumVertexFormat;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CompactChunkVertex.class)
public class CompactChunkVertexMixin {
	@Shadow
	@Final
	public static int STRIDE;

	@Unique
	private static final int GLOWTONE$GLOWTONE_STRIDE = 56;
	@Unique
	private static final int GLOWTONE$ADDITIONAL_STRIDE = GLOWTONE$GLOWTONE_STRIDE - STRIDE;
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
		final VertexFormat format = original.call(GTSodiumVertexFormat.appendTerrainAttributes(instance));
		GTSodiumVertexFormat.setupOffsets(format);
		return format;
	}

	@Inject(method = "lambda$getEncoder$0", at = @At("HEAD"))
	private static void glowtone$preWrite(
		long ptr, int materialBits, ChunkVertexEncoder.Vertex[] vertices, int section, CallbackInfoReturnable<Long> info,
		@Share("glowtone$state") LocalRef<ChromaBaker.SectionState> stateRef,
		@Share("glowtone$edges") LocalRef<QuadEdges> edgesRef,
		@Share("glowtone$fluid") LocalBooleanRef fluidRef,
		@Share("glowtone$flags") LocalIntRef flagsRef
	) {
		final ChromaBaker.SectionState state = ChromaBaker.state();
		stateRef.set(state);
		edgesRef.set(state.pendingEdges());
		fluidRef.set(state.fluidQuad());
		flagsRef.set(state.emissiveQuad() ? 0x000000FF : 0);
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
		@Share("glowtone$edges") LocalRef<QuadEdges> edgesRef,
		@Share("glowtone$fluid") LocalBooleanRef fluidRef,
		@Share("glowtone$flags") LocalIntRef flagsRef
	) {
		MemoryUtil.memPutInt(ptr + GTSodiumVertexFormat.CHROMA_OFFSET, ARGB.toABGR(stateRef.get().sample(vertex.x, vertex.y, vertex.z)));
		MemoryUtil.memPutInt(ptr + GTSodiumVertexFormat.SKY_CHROMA_OFFSET, GLOWTONE$SKY_CHROMA_ABGR);
		MemoryUtil.memPutInt(ptr + GTSodiumVertexFormat.FLAGS_OFFSET, flagsRef.get());

		final int edgeIndex = fluidRef.get() ? edgesRef.get().indexOf(vertex.x, vertex.y, vertex.z) : stateRef.get().nextEdgeVertex();
		glowtone$writeEdges(ptr, edgesRef.get(), edgeIndex);
	}

	@ModifyExpressionValue(
		method = "lambda$getEncoder$0",
		at = @At(
			value = "CONSTANT",
			args = "longValue=20"
		)
	)
	private static long glowtone$additionalStride(long original) {
		return original + GLOWTONE$ADDITIONAL_STRIDE;
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
	private static void glowtone$writeEdges(long pointer, QuadEdges edges, int index) {
		if (index < 0) {
			MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.EDGE_OFFSET, GLOWTONE$NO_EDGES_LE);
			MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.EDGE_MASK_OFFSET, 0);
			for (int contact = 0; contact < GLOWTONE$CONTACTS; contact++) {
				MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.CONTACT0_OFFSET + contact * 4L, GLOWTONE$NO_CONTACT_LE[contact]);
			}
			return;
		}

		MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.EDGE_OFFSET, Integer.reverseBytes(edges.get(index)));
		MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.EDGE_MASK_OFFSET, Integer.reverseBytes(edges.mask(index)));
		for (int contact = 0; contact < GLOWTONE$CONTACTS; contact++) {
			MemoryUtil.memPutInt(pointer + GTSodiumVertexFormat.CONTACT0_OFFSET + contact * 4L, Integer.reverseBytes(edges.contact(contact)));
		}
	}
}
