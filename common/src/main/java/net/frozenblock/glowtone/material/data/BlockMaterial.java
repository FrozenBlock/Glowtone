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

package net.frozenblock.glowtone.material.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.lib.block.api.attachment.BlockAttachmentKey;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ClientOnly
public record BlockMaterial(
	Optional<MaterialLayer> layer,
	Cull cull,
	Optional<MaterialRenderShape> renderShape,
	Optional<Boolean> blockEntityRender,
	Optional<MaterialShader> shader,
	List<String> target
) {
	public static final BlockMaterial NONE = new BlockMaterial(
		Optional.empty(),
		Cull.AUTOMATIC,
		Optional.empty(),
		Optional.empty(),
		Optional.empty(),
		List.of()
	);
	public static final Assigned UNASSIGNED = new Assigned(null, NONE, BlockMaterialRenderer.NO_SHADER);
	public static final Simple EMPTY = new Simple(UNASSIGNED);
	public static final BlockAttachmentKey<Baked> ATTACHMENT_KEY = BlockAttachmentKey.create(true, () -> "Block Materials");
	public static final MapCodec<BlockMaterial> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		MaterialLayer.CODEC.optionalFieldOf("layer").forGetter(BlockMaterial::layer),
		Cull.CODEC.optionalFieldOf("cull", Cull.AUTOMATIC).forGetter(BlockMaterial::cull),
		MaterialRenderShape.CODEC.optionalFieldOf("render_shape").forGetter(BlockMaterial::renderShape),
		Codec.BOOL.optionalFieldOf("block_entity_render").forGetter(BlockMaterial::blockEntityRender),
		MaterialShader.CODEC.optionalFieldOf("shader").forGetter(BlockMaterial::shader),
		Codec.STRING.listOf().optionalFieldOf("target", List.of()).forGetter(BlockMaterial::target)
	).apply(instance, BlockMaterial::new));
	public static final Codec<BlockMaterial> CODEC = MAP_CODEC.codec();

	public boolean overridesLayer() {
		return this.layer.isPresent();
	}

	public boolean overridesFaceCulling() {
		return this.cull.overridesFaces();
	}

	public boolean overridesShader() {
		return this.shader.isPresent();
	}

	public boolean overridesRenderShape() {
		return this.renderShape.isPresent();
	}

	public boolean overridesBlockEntityRender() {
		return this.blockEntityRender.isPresent();
	}

	public boolean isNone() {
		return this.equals(NONE);
	}

	public BlockMaterial mergedOver(BlockMaterial under) {
		return new BlockMaterial(
			this.layer.or(under::layer),
			this.cull.mergedOver(under.cull),
			this.renderShape.or(under::renderShape),
			this.blockEntityRender.or(under::blockEntityRender),
			this.shader.or(under::shader),
			this.target.isEmpty() ? under.target() : this.target
		);
	}

	public abstract static class Baked {
		public abstract Assigned get(BlockState state);
	}

	public static final class Simple extends Baked {
		private final Assigned assigned;

		public Simple(Assigned assigned) {
			this.assigned = assigned;
		}

		@Override
		public Assigned get(BlockState state) {
			return this.assigned;
		}
	}

	public static final class MultiVariant extends Baked {
		private final Map<BlockState, Assigned> map;

		public MultiVariant(Map<BlockState, Assigned> map) {
			this.map = map;
		}

		@Override
		public Assigned get(BlockState state) {
			return this.map.getOrDefault(state, UNASSIGNED);
		}
	}

	public record Assigned(
		@Nullable Identifier id,
		BlockMaterial material,
		int shaderIndex,
		@Nullable List<String> targetSlots
	) {
		public Assigned(@Nullable Identifier id, BlockMaterial material, int shaderIndex) {
			this(id, material, shaderIndex, null);
		}

		public boolean targeted() {
			return this.targetSlots != null;
		}
	}

	public record Cull(Optional<CullMode> self, Optional<CullMode> cast) {
		public static final Cull AUTOMATIC = new Cull(Optional.empty(), Optional.empty());
		public static final Codec<Cull> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			CullMode.CODEC.optionalFieldOf("self").forGetter(Cull::self),
			CullMode.CODEC.optionalFieldOf("cast").forGetter(Cull::cast)
		).apply(instance, Cull::new));

		public CullMode selfMode() {
			return this.self.orElse(CullMode.AUTO);
		}

		public CullMode castMode() {
			return this.cast.orElse(CullMode.AUTO);
		}

		public boolean overridesFaces() {
			return this.selfMode().decides() || this.castMode().decides();
		}

		public Cull mergedOver(Cull under) {
			return new Cull(
				this.self.or(under::self),
				this.cast.or(under::cast)
			);
		}
	}
}
