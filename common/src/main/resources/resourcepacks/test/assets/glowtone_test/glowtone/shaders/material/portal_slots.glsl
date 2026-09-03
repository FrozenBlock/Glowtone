vec3 acc = glowtone_sampleSlot(Sampler0, end_sky, glowtone_ScreenProj.xy / glowtone_ScreenProj.w * 0.15).rgb * 0.4;
for (int i = 0; i < PORTAL_LAYERS; i++) {
	float depth = 0.1 + float(i) * 0.07;
	vec2 uv = (glowtone_ScreenProj.xy / glowtone_ScreenProj.w) / depth + glowtone_GameTime * 60.0 * (0.3 + float(i) * 0.05);
	acc += glowtone_sampleSlot(Sampler0, end_portal, uv).rgb * (0.08 + 0.02 * float(i));
}
return vec4(acc, glowtone_Color.a);
