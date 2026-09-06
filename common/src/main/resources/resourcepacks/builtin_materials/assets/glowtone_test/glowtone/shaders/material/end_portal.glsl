	const vec3 GLOWTONE_COLORS[16] = vec3[16](
		vec3(0.022087, 0.098399, 0.110818),
		vec3(0.011892, 0.095924, 0.089485),
		vec3(0.027636, 0.101689, 0.100326),
		vec3(0.046564, 0.109883, 0.114838),
		vec3(0.064901, 0.117696, 0.097189),
		vec3(0.063761, 0.086895, 0.123646),
		vec3(0.084817, 0.111994, 0.166380),
		vec3(0.097489, 0.154120, 0.091064),
		vec3(0.106152, 0.131144, 0.195191),
		vec3(0.097721, 0.110188, 0.187229),
		vec3(0.133516, 0.138278, 0.148582),
		vec3(0.070006, 0.243332, 0.235792),
		vec3(0.196766, 0.142899, 0.214696),
		vec3(0.047281, 0.315338, 0.321970),
		vec3(0.204675, 0.390010, 0.302066),
		vec3(0.080955, 0.314821, 0.661491)
	);

	const mat4 GLOWTONE_SCALE_TRANSLATE = mat4(
		0.5, 0.0, 0.0, 0.25,
		0.0, 0.5, 0.0, 0.25,
		0.0, 0.0, 1.0, 0.0,
		0.0, 0.0, 0.0, 1.0
	);

	vec3 glowtone_sum = textureProj(Sky, glowtone_ScreenProj).rgb * GLOWTONE_COLORS[0];

	for (int glowtone_i = 0; glowtone_i < int(PORTAL_LAYERS); glowtone_i++) {
		float glowtone_layer = float(glowtone_i + 1);

		mat4 glowtone_translate = mat4(
			1.0, 0.0, 0.0, 17.0 / glowtone_layer,
			0.0, 1.0, 0.0, (2.0 + glowtone_layer / 1.5) * (glowtone_GameTime * 1.5),
			0.0, 0.0, 1.0, 0.0,
			0.0, 0.0, 0.0, 1.0
		);

		float glowtone_angle = radians((glowtone_layer * glowtone_layer * 4321.0 + glowtone_layer * 9.0) * 2.0);
		mat2 glowtone_rotate = mat2(
			cos(glowtone_angle), -sin(glowtone_angle),
			sin(glowtone_angle), cos(glowtone_angle)
		);
		mat2 glowtone_scale = mat2((4.5 - glowtone_layer / 4.0) * 2.0);

		mat4 glowtone_matrix = mat4(glowtone_scale * glowtone_rotate) * glowtone_translate * GLOWTONE_SCALE_TRANSLATE;

		glowtone_sum += textureProj(Stars, glowtone_ScreenProj * glowtone_matrix).rgb
			* GLOWTONE_COLORS[glowtone_i];
	}

	return vec4(glowtone_sum, 1.0);
