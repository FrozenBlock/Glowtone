/*
 * Copyright 2025 FrozenBlock
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

package net.frozenblock.glowtone.render_type.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@ApiStatus.Internal
@Environment(EnvType.CLIENT)
public class BlockRenderTypeOverwrite {
	private final Block block;
	private final RenderTypeOverwrite renderTypeOverwrite;

	public BlockRenderTypeOverwrite(Block block, RenderTypeOverwrite renderTypeOverwrite) {
		this.block = block;
		this.renderTypeOverwrite = renderTypeOverwrite;
	}

	public Block getBlock() {
		return this.block;
	}

	public RenderTypeOverwrite getRenderTypeOverwrite() {
		return this.renderTypeOverwrite;
	}

	public RenderType getRenderType() {
		return this.getRenderTypeOverwrite().get();
	}

	protected record RenderTypeOverwriteHolder(RenderTypeOverwrite renderTypeOverwrite) {
		public static final Codec<RenderTypeOverwriteHolder> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				RenderTypeOverwrite.CODEC.fieldOf("render_type").forGetter(RenderTypeOverwriteHolder::renderTypeOverwrite)
			).apply(instance, RenderTypeOverwriteHolder::new)
		);
	}

	public enum RenderTypeOverwrite implements StringRepresentable {
		SOLID("solid", RenderType::solid),
		CUTOUT_MIPPED("cutout_mipped", RenderType::cutoutMipped),
		CUTOUT("cutout", RenderType::cutout),
		TRANSLUCENT("translucent", RenderType::translucent),
		TRIPWIRE("tripwire", RenderType::tripwire);
		public static final Codec<RenderTypeOverwrite> CODEC = StringRepresentable.fromEnum(RenderTypeOverwrite::values);

		private final String name;
		private final Supplier<RenderType> renderTypeSupplier;

		RenderTypeOverwrite(String name, Supplier<RenderType> renderTypeSupplier) {
			this.name = name;
			this.renderTypeSupplier = renderTypeSupplier;
		}

		public RenderType get() {
			return this.renderTypeSupplier.get();
		}

		@Override
		public @NotNull String getSerializedName() {
			return this.name;
		}
	}
}
