/*
 * Copyright 2026 FrozenBlock
 * This file is part of Glowtone.
 *
 * This program is free software; you can modify it under
 * the terms of version 1 of the FrozenBlock Modding Oasis License
 * as published by FrozenBlock Modding Oasis.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * FrozenBlock Modding Oasis License for more details.
 *
 * You should have received a copy of the FrozenBlock Modding Oasis License
 * along with this program; if not, see <https://github.com/FrozenBlock/Licenses>.
 */

package net.frozenblock.glowtone.material;

import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import java.util.Map;

@ClientOnly
public final class BlockMaterials {
	public static final String RESOURCE_PACK_DIRECTORY = "glowtone/block_materials";
	public static final String OVERRIDE_DIRECTORY = "glowtone/block_material_overrides";
	public static final BlockAttachmentKey<Baked> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Block Materials");
	public static final int NO_SHADER = 0;
	public static final int MAX_SHADER_INDEX = 127;
	public static final int SHADER_INDEX_SHIFT = 24;
	public static final int GUI_MARKER = 0x2000;

	public static final Assigned UNASSIGNED = new Assigned(null, BlockMaterial.NONE, NO_SHADER);
	public static final Simple EMPTY = new Simple(UNASSIGNED);

	public record Assigned(@Nullable Identifier id, BlockMaterial material, int shaderIndex) {}

	private static volatile boolean anyLayers;
	private static volatile boolean anySelfCulling;
	private static volatile boolean anyCastCulling;
	private static volatile boolean anyShaders;
	private static volatile boolean anyRenderShape;
	private static volatile boolean anyBlockEntity;
	private static volatile boolean any;

	private static final int STACK_DEPTH = 8;

	private static final class State {
		private final Assigned[] stack = new Assigned[STACK_DEPTH];
		private int depth;
		private boolean gui;

		private @Nullable BlockState lastState;
		private Assigned lastAssigned = UNASSIGNED;

		State() {
			java.util.Arrays.fill(this.stack, UNASSIGNED);
		}

		private int seenGeneration = -1;

		Assigned resolve(BlockState state) {
			if (this.seenGeneration != generation) {
				this.seenGeneration = generation;
				this.lastState = null;
			}

			if (state != this.lastState) {
				this.lastState = state;
				this.lastAssigned = assigned(state);
			}

			return this.lastAssigned;
		}
	}

	private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

	private static void push(Assigned assigned) {
		push(STATE.get(), assigned);
	}

	private static void push(State state, Assigned assigned) {
		if (state.depth >= STACK_DEPTH - 1) return;

		state.stack[++state.depth] = assigned;
	}

	private static void pop() {
		final State state = STATE.get();
		if (state.depth > 0) state.depth--;
	}

	private static Assigned current() {
		final State state = STATE.get();
		return state.stack[state.depth];
	}

	public static Assigned assigned(BlockState state) {
		if (!any) return UNASSIGNED;

		final Baked baked = state.getBlock().frozenLib$getAttachedOrDefault(ATTACHMENT_KEY, EMPTY);
		return baked == null ? UNASSIGNED : baked.get(state);
	}

	public static BlockMaterial forBlockState(BlockState state) {
		return assigned(state).material();
	}

	public static void beginBlock(BlockState blockState) {
		if (!any) return;

		final State state = STATE.get();
		push(state, state.resolve(blockState));
	}

	public static void endBlock() {
		if (!any) return;
		pop();
	}

	public static BlockMaterial rendered() {
		return any ? current().material() : BlockMaterial.NONE;
	}

	public static int renderedShaderIndex() {
		return anyShaders ? current().shaderIndex() : NO_SHADER;
	}

	private static final Assigned[] INDEXED = new Assigned[MAX_SHADER_INDEX + 1];

	static {
		INDEXED[NO_SHADER] = UNASSIGNED;
		for (int index = 1; index <= MAX_SHADER_INDEX; index++) INDEXED[index] = new Assigned(null, BlockMaterial.NONE, index);
	}

	private static Assigned indexed(int shaderIndex) {
		return shaderIndex <= NO_SHADER || shaderIndex > MAX_SHADER_INDEX ? UNASSIGNED : INDEXED[shaderIndex];
	}

	public static void beginGui(boolean gui) {
		if (!anyShaders) return;
		STATE.get().gui = gui;
	}

	public static int markGui(int lightCoords) {
		return anyShaders && STATE.get().gui ? lightCoords | GUI_MARKER : lightCoords;
	}

	public static int markQuad(int lightCoords) {
		if (!anyShaders) return lightCoords;

		final State state = STATE.get();
		final int index = state.stack[state.depth].shaderIndex();
		final int marked = index == NO_SHADER ? lightCoords : lightCoords | (index << SHADER_INDEX_SHIFT);

		return state.gui ? marked | GUI_MARKER : marked;
	}

	public static int shaderIndexFor(BlockState state) {
		return anyShaders ? STATE.get().resolve(state).shaderIndex() : NO_SHADER;
	}

	public static void beginShaderIndex(int shaderIndex) {
		if (!anyShaders) return;
		push(indexed(shaderIndex));
	}

	public static void setShaderIndex(int shaderIndex) {
		if (!anyShaders) return;

		final State state = STATE.get();
		state.stack[state.depth] = indexed(shaderIndex);
	}

	public static int markShaderIndex(int lightCoords, int shaderIndex) {
		return shaderIndex == NO_SHADER ? lightCoords : lightCoords | (shaderIndex << SHADER_INDEX_SHIFT);
	}

	public static int markShaderIndex(int lightCoords) {
		if (!anyShaders) return lightCoords;

		final int index = current().shaderIndex();
		if (index == NO_SHADER) return lightCoords;

		return lightCoords | (index << SHADER_INDEX_SHIFT);
	}

	public static @Nullable ChunkSectionLayer overrideLayer() {
		if (!anyLayers) return null;

		final MaterialLayer override = rendered().layer().orElse(null);
		return override == null ? null : override.vanilla();
	}

	public static ChunkSectionLayer layer(ChunkSectionLayer baked) {
		final ChunkSectionLayer override = overrideLayer();
		return override == null ? baked : override;
	}

	private static volatile int generation;

	public static void setLoadedFeatures(
		boolean layers, boolean selfCulling, boolean castCulling, boolean shaders,
		boolean renderShape, boolean blockEntity
	) {
		anyLayers = layers;
		anySelfCulling = selfCulling;
		anyCastCulling = castCulling;
		anyShaders = shaders;
		anyRenderShape = renderShape;
		anyBlockEntity = blockEntity;
		any = layers || selfCulling || castCulling || shaders || renderShape || blockEntity;
		generation++;
	}

	public static boolean any() {
		return any;
	}

	public static boolean anyLayers() {
		return anyLayers;
	}

	public static boolean anyFaceCulling() {
		return anySelfCulling || anyCastCulling;
	}

	public static boolean anySelfCulling() {
		return anySelfCulling;
	}

	public static boolean anyCastCulling() {
		return anyCastCulling;
	}

	public static boolean anyShaders() {
		return anyShaders;
	}

	public static RenderShape renderShape(BlockState state, RenderShape baked) {
		if (!anyRenderShape) return baked;

		return forBlockState(state).renderShape().map(MaterialRenderShape::shape).orElse(baked);
	}

	public static boolean blockEntityRender(BlockState state) {
		if (!anyBlockEntity) return true;

		return forBlockState(state).blockEntityRender().orElse(true);
	}

	public abstract static class Baked {
		abstract Assigned get(BlockState state);
	}

	public static final class Simple extends Baked {
		private final Assigned assigned;

		public Simple(Assigned assigned) {
			this.assigned = assigned;
		}

		@Override
		Assigned get(BlockState state) {
			return this.assigned;
		}
	}

	public static final class MultiVariant extends Baked {
		private final Map<BlockState, Assigned> map;

		public MultiVariant(Map<BlockState, Assigned> map) {
			this.map = map;
		}

		@Override
		Assigned get(BlockState state) {
			return this.map.getOrDefault(state, UNASSIGNED);
		}
	}

	private BlockMaterials() {}
}
