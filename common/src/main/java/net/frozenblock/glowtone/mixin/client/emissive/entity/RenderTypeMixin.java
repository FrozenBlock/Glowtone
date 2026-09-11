package net.frozenblock.glowtone.mixin.client.emissive.entity;

import java.util.Optional;
import net.frozenblock.glowtone.bloom.shader.EmissiveShaderPatcher;
import net.frozenblock.glowtone.entity.RenderTypeTextureValidityCache;
import net.frozenblock.glowtone.entity.impl.GTEmissiveRenderType;
import net.frozenblock.glowtone.render.rendertype.GTRenderTypes;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@ClientOnly
@Mixin(RenderType.class)
public class RenderTypeMixin implements GTEmissiveRenderType {
	@Unique
	private boolean glowtone$isEmissive = false;
	@Unique
	private Identifier glowtone$emissiveTexture = null;
	@Unique
	private Optional<RenderType> glowtone$emissiveRenderType = Optional.empty();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void glowtone$initRenderTypeWithEmissive(String name, RenderSetup state, CallbackInfo info) {
		if (!EmissiveShaderPatcher.isEntityShader(RenderType.class.cast(this).pipeline().getFragmentShader())
			|| name.equals(GTRenderTypes.ENTITY_EMISSIVE_OVERLAY_NAME)
		) return;

		final RenderSetup.TextureBinding sampler0Binding = state.textures.get("Sampler0");
		if (sampler0Binding != null) {
			final Identifier texture = sampler0Binding.location();
			if (!texture.getPath().contains("glowtone_emissive")) {
				this.glowtone$emissiveTexture = texture.withPath(path -> path.replace(".png", "_glowtone_emissive.png"));
				final RenderType emissiveRenderType = GTRenderTypes.entityEmissiveOverlay(this.glowtone$emissiveTexture);
				emissiveRenderType.glowtone$markEmissive();
				this.glowtone$emissiveRenderType = Optional.of(emissiveRenderType);
			}
		}
	}

	@Unique
	@Override
	public boolean glowtone$isEmissive() {
		return this.glowtone$isEmissive;
	}

	@Unique
	@Override
	public RenderType glowtone$markEmissive() {
		this.glowtone$isEmissive = true;
		this.glowtone$emissiveTexture = null;
		this.glowtone$emissiveRenderType = Optional.empty();
		return RenderType.class.cast(this);
	}

	@Unique
	@Override
	public boolean glowtone$isEmissiveResourceValid() {
		if (this.glowtone$emissiveTexture == null) return false;
		return RenderTypeTextureValidityCache.getOrComputeValidity(this.glowtone$emissiveTexture);
	}

	@Unique
	@Override
	public Optional<RenderType> glowtone$emissiveRenderType() {
		return this.glowtone$emissiveRenderType;
	}
}
