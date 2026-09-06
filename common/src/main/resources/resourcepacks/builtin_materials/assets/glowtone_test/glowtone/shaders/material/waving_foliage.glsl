const float GLOWTONE_PI = 3.14159265359;

vec3 animPos = (glowtone_BlockPos + glowtone_LocalPos) * GLOWTONE_PI * WAVE_SCALE;
float animTime = glowtone_GameTime * WAVE_SPEED;

float xOffset = sin(animPos.x + (animPos.y / 2.0) + animTime) / WAVE_SWAY;
float yOffset = (sin(animPos.y + ((animPos.x + animPos.z) / 4.0) + animTime) / WAVE_BOB)
	+ (cos(((animPos.x + animPos.z) / 2.0) + (animPos.y / 4.0) + animTime * 2.0) / WAVE_BOB);
float zOffset = cos(animPos.z + (animPos.y / 2.0) + animTime) / WAVE_SWAY;

return vec3(xOffset, yOffset, zOffset);
