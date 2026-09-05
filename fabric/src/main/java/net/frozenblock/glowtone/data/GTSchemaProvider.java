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

package net.frozenblock.glowtone.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.frozenblock.glowtone.material.render.BlockMaterialRenderer;
import net.frozenblock.glowtone.material.data.CullMode;
import net.frozenblock.glowtone.material.MaterialLayer;
import net.frozenblock.glowtone.material.data.MaterialRenderShape;
import net.frozenblock.glowtone.material.MaterialSamplers;
import net.frozenblock.glowtone.material.data.MaterialShader;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class GTSchemaProvider implements DataProvider {
	private static final String SCHEMA = "https://json-schema.org/draft/2020-12/schema";
	private static final String IDENTIFIER_PATTERN = "^([a-z0-9_.-]+:)?[a-z0-9_./-]+$";

	private final PackOutput.PathProvider pathProvider;

	public GTSchemaProvider(PackOutput output) {
		this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "schemas");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache) {
		return CompletableFuture.allOf(
			this.save(cache, "block_material", blockMaterial()),
			this.save(cache, "block_material_overrides", dispatcher(
				"Glowtone block material overrides",
				"Assigns a block material to the blockstates of one block.",
				assignment()
			)),
			this.save(cache, "block_light_properties", dispatcher(
				"Glowtone block light properties",
				"Light and ambient occlusion overrides for the blockstates of one block.",
				lightProperties()
			))
		);
	}

	private CompletableFuture<?> save(CachedOutput cache, String name, JsonObject schema) {
		final Path path = this.pathProvider.json(Identifier.fromNamespaceAndPath("glowtone", name));
		return DataProvider.saveStable(cache, schema, path);
	}

	private static JsonObject blockMaterial() {
		// CULL
		final Property cullSelf = entry(
			"self",
			enumOf(
				"Whether this block draws its own faces.",
				CullMode.values()
			)
		);
		final Property cullCast = entry(
			"cast",
			enumOf(
				"Whether this block hides the faces of its neighbours.",
				CullMode.values()
			)
		);

		final JsonObject cull = object("How this material culls faces against its neighbours.");
		cull.add("properties", properties(cullSelf, cullCast));
		cull.addProperty("additionalProperties", false);

		// SHADER
		final Property shaderFragment = entry(
			"fragment",
			identifier("Fragment source under " + MaterialShader.RESOURCE_PACK_DIRECTORY + ". Returns the vec4 fragment colour.")
		);
		final Property shaderVertex = entry(
			"vertex",
			identifier("Vertex source under " + MaterialShader.RESOURCE_PACK_DIRECTORY + ". Returns a vec3 displacement.")
		);
		final Property shaderTextures = entry(
			"textures",
			map(
				"Sampler name to texture path. At most " + MaterialSamplers.SLOTS + " distinct textures across every loaded material.",
				string(null)
			)
		);
		final Property shaderBlockTextures = entry(
			"block_textures",
			list(
				"Texture slot names declared on the block's own model, sampled off the block atlas. Each arrives in the snippet as a vec4 atlas rectangle.",
				string(null)
			)
		);
		final Property shaderConstants = entry(
			"constants",
			map(
				"Name to GLSL expression, emitted as a #define around the stage. Use for values that must be compile time, such as loop bounds.",
				string(null)
			)
		);
		final Property shaderParameters = entry(
			"parameters",
			map(
				"Name to GLSL float expression, passed to the snippet as an argument. Two materials whose snippets match share one compiled program even when their parameter values differ.",
				string(null)
			)
		);

		final JsonObject shader = object("Shader stages applied to this material.");
		shader.add("properties", properties(shaderFragment, shaderVertex, shaderTextures, shaderBlockTextures, shaderConstants, shaderParameters));
		shader.addProperty("additionalProperties", false);

		// MATERIAL
		final Property materialParent = entry("parent", identifier("Another material to inherit every unset field from."));
		final Property materialLayer = entry("layer", layer());
		final Property materialRenderShape = entry("render_shape", enumOf("Replaces the render shape of the block.", MaterialRenderShape.values()));
		final Property materialBlockEntityRender = entry("block_entity_render", bool("Set false to suppress the block entity renderer for this block."));
		final Property materialCull = entry("cull", cull);
		final Property materialTarget = entry(
			"target",
			list(
				"Model texture slot names this material applies to. When set, only quads baked from those slots are affected, so an overlay element can be shaded while the rest of the block is left alone.",
				string(null)
			)
		);
		final Property materialShader = entry("shader", shader);

		final JsonObject root = object("A Glowtone block material.");
		root.addProperty("$schema", SCHEMA);
		root.addProperty("title", "Glowtone block material");
		root.add("properties", properties(materialParent, materialLayer, materialRenderShape, materialBlockEntityRender, materialCull, materialTarget, materialShader));
		root.addProperty("additionalProperties", false);
		return root;
	}

	private static JsonObject lightProperties() {
		// AMBIENT OCCLUSION
		final Property occlusionSelf = entry("self", bool("Whether this block receives ambient occlusion."));
		final Property occlusionCast = entry("cast", bool("Whether this block casts ambient occlusion onto its neighbours."));

		final JsonObject occlusion = object("Ambient occlusion overrides.");
		occlusion.add("properties", properties(occlusionSelf, occlusionCast));
		occlusion.addProperty("additionalProperties", false);

		// EMISSIVEN
		final Property emissiveBrightness = entry("brightness", integer("Emissive brightness.", 0, LightEngine.MAX_LEVEL));
		final Property emissiveBloom = entry("bloom", bool("Whether this block contributes to bloom."));

		final JsonObject emissive = object("Emissive and bloom overrides.");
		emissive.add("properties", properties(emissiveBrightness, emissiveBloom));
		emissive.addProperty("additionalProperties", false);

		// LIGHT PROPERTIES
		final Property lightPropertiesLightColor = entry("light_color", integer("Packed RGB colour of the light this block emits.", null, null));
		final Property lightPropertiesLightColorFilter = entry(
			"light_filter_color",
			integer("Packed RGB colour this block tints light passing through it.", null, null)
		);
		final Property lightPropertiesAmbientOcclusion = entry("ambient_occlusion", occlusion);
		final Property lightPropertiesEmissive = entry("emissive", emissive);

		final JsonObject root = object("Light properties for one blockstate.");
		root.add("properties", properties(lightPropertiesLightColor, lightPropertiesLightColorFilter, lightPropertiesAmbientOcclusion, lightPropertiesEmissive));
		root.addProperty("additionalProperties", false);
		return root;
	}

	private static JsonObject assignment() {
		// FULL
		final Property fullMaterial = entry("material", identifier("Material id, as defined under " + BlockMaterialRenderer.RESOURCE_PACK_DIRECTORY + "."));
		final Property fullParameters = entry("parameters", map("Name to GLSL float expression, overriding a parameter the material declares.", string(null)));

		final JsonObject full = object("A material plus parameter values that override the material's own.");
		full.add("properties", properties(fullMaterial, fullParameters));
		full.add("required", array("material"));
		full.addProperty("additionalProperties", false);

		// FORMS
		final JsonArray forms = new JsonArray();
		forms.add(identifier("Material id, as defined under " + BlockMaterialRenderer.RESOURCE_PACK_DIRECTORY + "."));
		forms.add(full);

		final JsonObject json = new JsonObject();
		json.add("oneOf", forms);
		return json;
	}

	private static JsonObject dispatcher(String title, String description, JsonObject value) {
		final JsonObject variants = object("Blockstate selector to value. An empty key matches every state.");
		variants.add("additionalProperties", value);

		final JsonObject root = object(description);
		root.addProperty("$schema", SCHEMA);
		root.addProperty("title", title);
		root.add("properties", properties(entry("variants", variants)));
		root.add("required", array("variants"));
		root.addProperty("additionalProperties", false);
		return root;
	}

	private static JsonObject layer() {
		final JsonObject json = enumValues(
			"Chunk layer this material draws in.",
			MaterialLayer.SOLID.id().getPath(),
			MaterialLayer.CUTOUT.id().getPath(),
			MaterialLayer.TRANSLUCENT.id().getPath()
		);
		json.addProperty("type", "string");
		return json;
	}

	private static JsonObject enumOf(String description, StringRepresentable[] values) {
		final String[] names = new String[values.length];
		for (int i = 0; i < values.length; i++) names[i] = values[i].getSerializedName();

		final JsonObject json = enumValues(description, names);
		json.addProperty("type", "string");
		return json;
	}

	private static JsonObject enumValues(String description, String... values) {
		final JsonObject json = new JsonObject();
		json.addProperty("description", description);
		json.add("enum", array(values));
		return json;
	}

	private static JsonObject identifier(String description) {
		final JsonObject json = string(description);
		json.addProperty("pattern", IDENTIFIER_PATTERN);
		return json;
	}

	private static JsonObject string(@Nullable String description) {
		final JsonObject json = new JsonObject();
		json.addProperty("type", "string");
		if (description != null) json.addProperty("description", description);
		return json;
	}

	private static JsonObject bool(String description) {
		final JsonObject json = new JsonObject();
		json.addProperty("type", "boolean");
		json.addProperty("description", description);
		return json;
	}

	private static JsonObject integer(String description, @Nullable Integer min, @Nullable Integer max) {
		final JsonObject json = new JsonObject();
		json.addProperty("type", "integer");
		json.addProperty("description", description);
		if (min != null) json.addProperty("minimum", min);
		if (max != null) json.addProperty("maximum", max);
		return json;
	}

	private static JsonObject map(String description, JsonObject value) {
		final JsonObject json = object(description);
		json.add("additionalProperties", value);
		return json;
	}

	private static JsonObject list(String description, JsonObject element) {
		final JsonObject json = new JsonObject();
		json.addProperty("type", "array");
		json.addProperty("description", description);
		json.add("items", element);
		return json;
	}

	private static JsonObject object(@Nullable String description) {
		final JsonObject json = new JsonObject();
		json.addProperty("type", "object");
		if (description != null) json.addProperty("description", description);
		return json;
	}

	private static JsonArray array(String... values) {
		final JsonArray json = new JsonArray();
		for (String value : values) json.add(value);
		return json;
	}

	private record Property(String name, JsonObject schema) {}

	private static Property entry(String name, JsonObject schema) {
		return new Property(name, schema);
	}

	private static JsonObject properties(Property... entries) {
		final JsonObject json = new JsonObject();
		for (Property entry : entries) json.add(entry.name(), entry.schema());
		return json;
	}

	@Override
	public String getName() {
		return "Glowtone Pack Schemas";
	}
}
