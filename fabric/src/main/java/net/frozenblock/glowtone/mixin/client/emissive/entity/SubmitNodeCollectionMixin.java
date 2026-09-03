package net.frozenblock.glowtone.mixin.client.emissive.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.frozenblock.glowtone.emissive.entity.impl.GTEmissiveSubmitNodeCollection;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(value = SubmitNodeCollection.class, priority = 950)
public abstract class SubmitNodeCollectionMixin implements GTEmissiveSubmitNodeCollection {
	@Unique
	public final TranslucentFeatureRenderPhase glowtone$emissiveModelOverlays = new TranslucentFeatureRenderPhase();

	@Shadow
	@Final
	public TranslucentFeatureRenderPhase translucentModels;
	@Shadow
	@Final
	@Mutable
	private List<FeatureRenderPhase<?>> allPhases;

	@Shadow
	public abstract <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay);

	@Inject(method = "<init>", at = @At("TAIL"))
	public void glowtone$addEmissiveModelOverlaysToAllPhases(CallbackInfo info) {
		final List<FeatureRenderPhase<?>> allPhases = new ArrayList<>();
		for (FeatureRenderPhase<?> phase : this.allPhases) {
			allPhases.add(phase);
			if (phase == this.translucentModels) allPhases.add(this.glowtone$emissiveModelOverlays);
		}
		this.allPhases = List.copyOf(allPhases);
	}

	@Unique
	@Override
	public TranslucentFeatureRenderPhase glowtone$emissiveModelOverlays() {
		return this.glowtone$emissiveModelOverlays;
	}

	@WrapOperation(
		method = "submitModel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;translucentModels:Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;",
				opcode = Opcodes.GETFIELD
			)
		)
	)
	public <S> void glowtone$submitEmissiveWhenTranslucent(
		TranslucentFeatureRenderPhase instance, TranslucentSubmit submit, Operation<Void> original,
		@Local(argsOnly = true) Model<? super S> model,
		@Local(argsOnly = true) S state,
		@Local(argsOnly = true) PoseStack poseStack,
		@Local(argsOnly = true) RenderType renderType,
		@Local(argsOnly = true, ordinal = 0) int lightCoords,
		@Local(argsOnly = true, ordinal = 1) int overlayCoords,
		@Local(argsOnly = true, ordinal = 2) int tintedColor,
		@Local(argsOnly = true) @Nullable TextureAtlasSprite sprite,
		@Local(argsOnly = true, ordinal = 3) int outlineColor
	) {
		if (renderType.glowtone$isEmissive()) {
			this.glowtone$emissiveModelOverlays.submit(submit);
			return;
		}

		original.call(instance, submit);
		this.glowtone$submitEmissiveOverlay(model, state, poseStack, renderType, lightCoords, tintedColor, sprite, outlineColor);
	}

	@WrapOperation(
		method = "submitModel",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lnet/minecraft/client/renderer/SubmitNodeCollection;solid:Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;",
				opcode = Opcodes.GETFIELD
			)
		)
	)
	public <S> void glowtone$submitEmissiveWhenSolid(
		SimpleFeatureRenderPhase instance, SubmitNode submit, Operation<Void> original,
		@Local(argsOnly = true) Model<? super S> model,
		@Local(argsOnly = true) S state,
		@Local(argsOnly = true) PoseStack poseStack,
		@Local(argsOnly = true) RenderType renderType,
		@Local(argsOnly = true, ordinal = 0) int lightCoords,
		@Local(argsOnly = true, ordinal = 1) int overlayCoords,
		@Local(argsOnly = true, ordinal = 2) int tintedColor,
		@Local(argsOnly = true) @Nullable TextureAtlasSprite sprite,
		@Local(argsOnly = true, ordinal = 3) int outlineColor
	) {
		original.call(instance, submit);
		this.glowtone$submitEmissiveOverlay(model, state, poseStack, renderType, lightCoords, tintedColor, sprite, outlineColor);
	}

	@Unique
	private <S> void glowtone$submitEmissiveOverlay(
		Model<? super S> model,
		S state,
		PoseStack poseStack,
		RenderType renderType,
		int lightCoords,
		int tintedColor,
		@Nullable TextureAtlasSprite sprite,
		int outlineColor
	) {
		if (renderType.glowtone$isEmissive() || !renderType.glowtone$isEmissiveResourceValid()) return;

		final RenderType emissiveRenderType = glowtone$getEmissiveRenderType(renderType);
		if (emissiveRenderType == null) return;

		this.submitModel(model, state, poseStack, emissiveRenderType, lightCoords, OverlayTexture.NO_OVERLAY, tintedColor, sprite, outlineColor, null);
	}

	@Unique
	@Nullable
	private static RenderType glowtone$getEmissiveRenderType(RenderType renderType) {
		return renderType.glowtone$emissiveRenderType().isPresent() ? renderType.glowtone$emissiveRenderType().get() : null;
	}
}
