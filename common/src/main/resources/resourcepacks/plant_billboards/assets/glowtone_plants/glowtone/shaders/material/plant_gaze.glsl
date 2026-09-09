if (glowtone_Context != 0) return vec3(0.0);

vec3 stem = vec3(PLANT_STEM_X, PLANT_STEM_Y, PLANT_STEM_Z);
bool upright = abs(stem.y) >= max(abs(stem.x), abs(stem.z));

vec3 planeU = (upright || abs(stem.z) >= abs(stem.x)) ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 0.0, 1.0);
vec3 planeV = upright ? vec3(0.0, 0.0, 1.0) : vec3(0.0, 1.0, 0.0);

vec3 corner = upright
	? vec3(glowtone_QuadOffset.x, 0.0, glowtone_QuadOffset.y)
	: glowtone_LocalPos - 0.5;
vec3 pivot = upright
	? glowtone_BlockPos + glowtone_LocalPos - corner
	: glowtone_BlockPos + 0.5;

vec2 offset = vec2(dot(corner, planeU), dot(corner, planeV));
if (length(offset) < 1.0e-4) return vec3(0.0);

vec3 eye = glowtone_CameraPos - pivot;
vec2 toCamera = vec2(dot(eye, planeU), dot(eye, planeV));
float distance = length(toCamera);
if (distance < 1.0e-4) return vec3(0.0);

vec2 facing = toCamera / distance;
vec2 across = vec2(-facing.y, facing.x);

vec2 aligned = vec2(dot(glowtone_CameraRight, planeU), dot(glowtone_CameraRight, planeV));
if (PLANT_ALIGN > 0.5 && length(aligned) > 1.0e-4) across = normalize(aligned);

vec2 authored = (abs(PLANT_AXIS_X) + abs(PLANT_AXIS_Z)) > 1.0e-4
	? vec2(PLANT_AXIS_X, PLANT_AXIS_Z)
	: offset;
float reference = mod(atan(authored.y, authored.x), 1.57079633);

float turn = atan(across.y, across.x) - reference;
float cosine = cos(turn);
float sine = sin(turn);

vec2 turned = vec2(
	offset.x * cosine - offset.y * sine,
	offset.x * sine + offset.y * cosine
);

vec2 shift = (turned - offset) * PLANT_STRENGTH;
return shift.x * planeU + shift.y * planeV;
