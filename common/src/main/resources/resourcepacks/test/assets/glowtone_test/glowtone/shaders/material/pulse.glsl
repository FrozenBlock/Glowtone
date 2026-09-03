float wave = sin(glowtone_WorldPos.y * PULSE_SCALE + glowtone_GameTime * PULSE_SPEED) * 0.5 + 0.5;
return vec4(glowtone_Color.rgb * (0.55 + 0.45 * wave), glowtone_Color.a);
