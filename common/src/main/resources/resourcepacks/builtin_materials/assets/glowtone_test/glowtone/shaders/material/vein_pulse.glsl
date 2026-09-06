// Continuous position, so the wave crosses block boundaries without a visible step at each tile.
vec3 at = glowtone_BlockPos + glowtone_LocalPos;

// Travels upward. x and z only skew the phase, so neighbouring columns are offset rather than in lockstep.
float rise = -at.y * PULSE_SCALE_Y + at.x * PULSE_SKEW_X + at.z * PULSE_SKEW_Z;
float time = glowtone_GameTime * PULSE_SPEED;

// Three octaves at non-integer ratios: no common period, so the pattern does not visibly repeat and the
// crests vary in width and spacing instead of forming even bands.
float wave = sin(rise + time);
wave += 0.55 * sin(rise * 2.17 + time * 1.31 + 1.7);
wave += 0.30 * sin(rise * 4.61 + time * 0.74 + 4.2);
wave /= 1.85;

float pulse = clamp(wave * 0.5 + 0.5, 0.0, 1.0);
pulse = smoothstep(0.0, 1.0, pow(pulse, PULSE_SHARPNESS));

return vec4(glowtone_Color.rgb * mix(PULSE_MIN, PULSE_MAX, pulse), glowtone_Color.a);
