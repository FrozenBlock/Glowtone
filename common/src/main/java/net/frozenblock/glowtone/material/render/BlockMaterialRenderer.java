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

package net.frozenblock.glowtone.material.render;

import com.mojang.logging.LogUtils;
import net.frozenblock.glowtone.GlowtoneConstants;
import net.frozenblock.glowtone.data.BlockMaterial;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.impl.BlockMaterialAttachment;
import net.frozenblock.glowtone.data.MaterialRenderShape;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@ClientOnly
public final class BlockMaterialRenderer {
	public static final String RESOURCE_PACK_DIRECTORY = "glowtone/block_materials";
	public static final String OVERRIDE_DIRECTORY = "glowtone/block_material_overrides";
	public static final int NO_SHADER = 0;
	public static final int MAX_SHADER_INDEX = 255;
	public static final int SHADER_INDEX_SHIFT = 24;
	public static final int GUI_MARKER = 0x2000;

	private static volatile boolean anyLayers;
	private static volatile boolean anySelfCulling;
	private static volatile boolean anyCastCulling;
	private static volatile boolean anyShaders;
	private static volatile boolean anyTargets;
	private static volatile boolean anyRenderShape;
	private static volatile boolean anyBlockEntity;
	private static volatile boolean any;

	private static final int STACK_DEPTH = 8;

	private static final class State {
		private final BlockMaterial.Assigned[] stack = new BlockMaterial.Assigned[STACK_DEPTH];
		private int depth;
		private boolean gui;

		private @Nullable BlockState lastState;
		private BlockMaterial.Assigned lastAssigned = BlockMaterial.UNASSIGNED;

		State() {
			java.util.Arrays.fill(this.stack, BlockMaterial.UNASSIGNED);
		}

		private int quadIndex = NO_SHADER;
		private int seenGeneration = -1;

		BlockMaterial.Assigned resolve(BlockState state) {
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

	private static void push(BlockMaterial.Assigned assigned) {
		push(STATE.get(), assigned);
	}

	private static void push(State state, BlockMaterial.Assigned assigned) {
		if (state.depth >= STACK_DEPTH - 1) return;

		state.stack[++state.depth] = assigned;
		state.quadIndex = assigned.shaderIndex();
	}

	private static void pop() {
		final State state = STATE.get();
		if (state.depth > 0) state.depth--;
		state.quadIndex = state.stack[state.depth].shaderIndex();
	}

	private static BlockMaterial.Assigned current() {
		final State state = STATE.get();
		return state.stack[state.depth];
	}

	public static BlockMaterial.Assigned assigned(BlockState state) {
		if (!any) return BlockMaterial.UNASSIGNED;

		return ((BlockMaterialAttachment) state.getBlock()).glowtone$getMaterial().get(state);
	}

	public static BlockMaterial forBlockState(BlockState state) {
		return assigned(state).material();
	}

	public static void beginBlock(BlockState blockState) {
		if (!any) return;

		final State state = STATE.get();
		final BlockMaterial.Assigned assigned = state.resolve(blockState);
		if (!reportedShaderBlock && assigned.shaderIndex() != NO_SHADER) reportShaderBlock(blockState, assigned);

		push(state, assigned);
	}

	private static volatile boolean reportedShaderBlock;

	private static synchronized void reportShaderBlock(BlockState blockState, BlockMaterial.Assigned assigned) {
		if (reportedShaderBlock) return;

		reportedShaderBlock = true;
		LogUtils.getLogger().info(
			"Glowtone meshed {} with shader material {} (index {})",
			blockState.getBlock(), assigned.id(), assigned.shaderIndex()
		);
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

	private static final BlockMaterial.Assigned[] INDEXED = new BlockMaterial.Assigned[MAX_SHADER_INDEX + 1];

	static {
		INDEXED[NO_SHADER] = BlockMaterial.UNASSIGNED;
		for (int index = 1; index <= MAX_SHADER_INDEX; index++) INDEXED[index] = new BlockMaterial.Assigned(null, BlockMaterial.NONE, index);
	}

	private static BlockMaterial.Assigned indexed(int shaderIndex) {
		if (shaderIndex <= NO_SHADER || shaderIndex > MAX_SHADER_INDEX) return BlockMaterial.UNASSIGNED;

		final BlockMaterial.Assigned assigned = ASSIGNED_BY_INDEX[shaderIndex];
		return assigned == null ? INDEXED[shaderIndex] : assigned;
	}

	private static final BlockMaterial.Assigned[] ASSIGNED_BY_INDEX = new BlockMaterial.Assigned[MAX_SHADER_INDEX + 1];

	public static void setAssignedByIndex(Map<Integer, BlockMaterial.Assigned> byIndex) {
		java.util.Arrays.fill(ASSIGNED_BY_INDEX, null);
		byIndex.forEach((index, assigned) -> {
			if (index > NO_SHADER && index <= MAX_SHADER_INDEX) ASSIGNED_BY_INDEX[index] = assigned;
		});
	}

	public static void beginQuad(BakedQuad quad) {
		if (!anyTargets) return;

		final State state = STATE.get();
		final BlockMaterial.Assigned assigned = state.stack[state.depth];
		if (!assigned.targeted()) {
			state.quadIndex = assigned.shaderIndex();
			return;
		}

		final TextureAtlasSprite sprite = quad.materialInfo().sprite();
		final boolean matched = assigned.targets(sprite) || (assigned.targetsEmissive() && isEmissiveOverlay(sprite));

		state.quadIndex = matched ? assigned.shaderIndex() : NO_SHADER;
	}

	// resolve the target from the quad's atlas position.
	public static void beginQuadAtlasCoord(float u, float v) {
		if (!anyTargets) return;

		STATE.get().quadIndex = indexForAtlasCoord(u, v);
	}

	public static int quadShaderIndex() {
		return anyShaders ? quadIndex(STATE.get()) : NO_SHADER;
	}

	public static int indexForAtlasCoord(float u, float v) {
		final BlockMaterial.Assigned assigned = current();
		final int index = assigned.shaderIndex();
		if (index == NO_SHADER || !anyTargets || !assigned.targeted()) return index;
		if (assigned.targetsEmissive() && BlockTextureSlots.withinEmissiveOverlay(u, v)) return index;

		return assigned.targets(u, v) ? index : NO_SHADER;
	}

	public static final String EMISSIVE_TARGET = "emissive";

	private static final Map<TextureAtlasSprite, Boolean> EMISSIVE_SPRITES = new ConcurrentHashMap<>();

	private static boolean isEmissiveOverlay(TextureAtlasSprite sprite) {
		return EMISSIVE_SPRITES.computeIfAbsent(
			sprite, key -> key.contents().name().getPath().endsWith(GlowtoneConstants.EMISSIVE_SUFFIX)
		);
	}

	public static void beginGui(boolean gui) {
		if (!anyShaders) return;
		STATE.get().gui = gui;
	}

	public static int markGui(int lightCoords) {
		return anyShaders && STATE.get().gui ? lightCoords | GUI_MARKER : lightCoords;
	}

	private static int quadIndex(State state) {
		final BlockMaterial.Assigned assigned = state.stack[state.depth];
		return assigned.targeted() ? state.quadIndex : assigned.shaderIndex();
	}

	public static int markQuad(int lightCoords) {
		if (!anyShaders) return lightCoords;

		final State state = STATE.get();
		final int index = quadIndex(state);
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
		final BlockMaterial.Assigned assigned = indexed(shaderIndex);
		state.stack[state.depth] = assigned;
		state.quadIndex = assigned.shaderIndex();
	}

	public static int markShaderIndex(int lightCoords, int shaderIndex) {
		return shaderIndex == NO_SHADER ? lightCoords : lightCoords | (shaderIndex << SHADER_INDEX_SHIFT);
	}

	public static int markShaderIndex(int lightCoords) {
		if (!anyShaders) return lightCoords;

		final State state = STATE.get();
		if (state.stack[state.depth].targeted()) return lightCoords;

		final int index = quadIndex(state);
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
		boolean renderShape, boolean blockEntity, boolean targets
	) {
		anyLayers = layers;
		anySelfCulling = selfCulling;
		anyCastCulling = castCulling;
		anyShaders = shaders;
		anyTargets = targets;
		EMISSIVE_SPRITES.clear();
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

	private BlockMaterialRenderer() {}
}
