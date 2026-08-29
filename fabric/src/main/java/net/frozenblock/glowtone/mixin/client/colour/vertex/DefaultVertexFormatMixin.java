package net.frozenblock.glowtone.mixin.client.colour.vertex;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.frozenblock.glowtone.render.vertex.GTDefaultVertexFormat;
import net.mehvahdjukaar.candlelight.api.ClientOnly;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@ClientOnly
@Mixin(DefaultVertexFormat.class)
public class DefaultVertexFormatMixin {

	@WrapOperation(
		method = "<clinit>",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexFormat$Builder;build()Lcom/mojang/blaze3d/vertex/VertexFormat;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;LINE_WIDTH_FORMAT:Lcom/mojang/blaze3d/GpuFormat;",
				opcode = Opcodes.PUTSTATIC
			)
		)
	)
	private static VertexFormat glowtone$modifyBlockVertexFormat(VertexFormat.Builder instance, Operation<VertexFormat> original) {
		final VertexFormat format = original.call(GTDefaultVertexFormat.appendTerrainAttributes(instance));
		GTDefaultVertexFormat.setupOffsets(format);
		return format;
	}
}
