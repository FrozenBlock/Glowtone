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

package net.frozenblock.glowtone.mixin.client.bloom;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.List;
import net.frozenblock.glowtone.bloom.GlowtoneEmissivePipeline;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@ClientOnly
@Mixin(RenderPass.class)
public class RenderPassMixin {
	@Shadow
	@Final
	private List<?> colorAttachments;

	@ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
	private RenderPipeline glowtone$matchEmissiveTargets(RenderPipeline pipeline) {
		final int attachments = this.colorAttachments.size();
		if (pipeline.getColorTargetStates().length == attachments) return pipeline;

		final RenderPipeline twin = GlowtoneEmissivePipeline.of(pipeline);
		return twin.getColorTargetStates().length == attachments ? twin : pipeline;
	}
}
